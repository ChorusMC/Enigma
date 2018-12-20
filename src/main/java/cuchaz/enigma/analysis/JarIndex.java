/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.analysis;

import com.google.common.collect.*;
import cuchaz.enigma.translation.representation.*;
import cuchaz.enigma.translation.representation.entry.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.util.*;

public class JarIndex {
	private final ReferencedEntryPool entryPool;

	private Set<ClassEntry> obfClassEntries;
	private TranslationIndex translationIndex;
	private Map<Entry<?>, AccessFlags> access;
	private Multimap<ClassEntry, FieldDefEntry> fields;
	private Multimap<ClassEntry, MethodDefEntry> methods;
	private Multimap<String, MethodDefEntry> methodImplementations;
	private Multimap<MethodEntry, EntryReference<MethodEntry, MethodDefEntry>> methodsReferencing;
	private Multimap<ClassEntry, EntryReference<ClassEntry, MethodDefEntry>> methodsReferencingClasses;
	private Multimap<MethodEntry, MethodEntry> methodReferences;
	private Multimap<FieldEntry, EntryReference<FieldEntry, MethodDefEntry>> fieldReferences;
	private Multimap<ClassEntry, ClassEntry> innerClassesByOuter;
	private Map<ClassEntry, ClassEntry> outerClassesByInner;
	private Map<MethodEntry, MethodEntry> bridgedMethods;
	private Set<MethodEntry> syntheticMethods;

	public JarIndex(ReferencedEntryPool entryPool) {
		this.entryPool = entryPool;
		this.obfClassEntries = Sets.newHashSet();
		this.translationIndex = new TranslationIndex(entryPool);
		this.access = Maps.newHashMap();
		this.fields = HashMultimap.create();
		this.methods = HashMultimap.create();
		this.methodImplementations = HashMultimap.create();
		this.methodsReferencingClasses = HashMultimap.create();
		this.methodsReferencing = HashMultimap.create();
		this.methodReferences = HashMultimap.create();
		this.fieldReferences = HashMultimap.create();
		this.innerClassesByOuter = HashMultimap.create();
		this.outerClassesByInner = Maps.newHashMap();
		this.bridgedMethods = Maps.newHashMap();
		this.syntheticMethods = Sets.newHashSet();
	}

	public void indexJar(ParsedJar jar, boolean buildInnerClasses) {
		// step 1: read the class names
		obfClassEntries.addAll(jar.getClassEntries());

		// step 2: index classes, fields, methods, interfaces
		if (buildInnerClasses) {
			// + step 5: index inner classes
			jar.visitReader(name -> new IndexClassVisitor(this, Opcodes.ASM5, new IndexInnerClassVisitor(this, Opcodes.ASM5)), ClassReader.SKIP_CODE);
		} else {
			jar.visitReader(name -> new IndexClassVisitor(this, Opcodes.ASM5), ClassReader.SKIP_CODE);
		}

		// step 3: index field, method, constructor references
		jar.visitReader(name -> new IndexReferenceVisitor(this, Opcodes.ASM5), ClassReader.SKIP_FRAMES);

		// step 4: index access and bridged methods
		for (MethodDefEntry methodEntry : methods.values()) {
			// look for access and bridged methods
			MethodEntry accessedMethod = findAccessMethod(methodEntry);
			if (accessedMethod != null) {
				if (isBridgedMethod(accessedMethod, methodEntry)) {
					this.bridgedMethods.put(methodEntry, accessedMethod);
				}
			}
		}
	}

	protected ClassDefEntry indexClass(int access, String name, String signature, String superName, String[] interfaces) {
		for (String interfaceName : interfaces) {
			if (name.equals(interfaceName)) {
				throw new IllegalArgumentException("Class cannot be its own interface! " + name);
			}
		}
		ClassDefEntry entry = this.translationIndex.indexClass(access, name, signature, superName, interfaces);
		this.access.put(entry, entry.getAccess());
		return entry;
	}

	protected void indexField(ClassDefEntry owner, int access, String name, String desc, String signature) {
		FieldDefEntry fieldEntry = new FieldDefEntry(owner, name, new TypeDescriptor(desc), Signature.createTypedSignature(signature), new AccessFlags(access));
		this.translationIndex.indexField(fieldEntry);
		this.access.put(fieldEntry, fieldEntry.getAccess());
		this.fields.put(fieldEntry.getParent(), fieldEntry);
	}

	protected void indexMethod(ClassDefEntry owner, int access, String name, String desc, String signature) {
		MethodDefEntry methodEntry = new MethodDefEntry(owner, name, new MethodDescriptor(desc), Signature.createSignature(signature), new AccessFlags(access));
		this.translationIndex.indexMethod(methodEntry);
		this.access.put(methodEntry, methodEntry.getAccess());
		this.methods.put(methodEntry.getParent(), methodEntry);

		if (new AccessFlags(access).isSynthetic()) {
			syntheticMethods.add(methodEntry);
		}

		// we don't care about constructors here
		if (!methodEntry.isConstructor()) {
			// index implementation
			this.methodImplementations.put(methodEntry.getParent().getFullName(), methodEntry);
		}
	}

	protected void indexMethodCall(MethodDefEntry callerEntry, String owner, String name, String desc) {
		ClassEntry referencedClass = entryPool.getClass(owner);
		MethodEntry referencedMethod = new MethodEntry(referencedClass, name, new MethodDescriptor(desc));
		ClassEntry resolvedClassEntry = translationIndex.resolveEntryOwner(referencedMethod);
		if (resolvedClassEntry != null && !resolvedClassEntry.equals(referencedMethod.getParent())) {
			referencedMethod = referencedMethod.withParent(resolvedClassEntry);
		}
		methodsReferencing.put(referencedMethod, new EntryReference<>(referencedMethod, referencedMethod.getName(), callerEntry));
		if (referencedMethod.isConstructor()) {
			methodsReferencingClasses.put(referencedClass, new EntryReference<>(referencedClass, referencedMethod.getName(), callerEntry));
		}
		methodReferences.put(callerEntry, referencedMethod);
	}

	protected void indexFieldAccess(MethodDefEntry callerEntry, String owner, String name, String desc) {
		FieldEntry referencedField = new FieldEntry(entryPool.getClass(owner), name, new TypeDescriptor(desc));
		ClassEntry resolvedClassEntry = translationIndex.resolveEntryOwner(referencedField);
		if (resolvedClassEntry != null && !resolvedClassEntry.equals(referencedField.getParent())) {
			referencedField = referencedField.withParent(resolvedClassEntry);
		}
		fieldReferences.put(referencedField, new EntryReference<>(referencedField, referencedField.getName(), callerEntry));
	}

	public void indexInnerClass(ClassEntry innerEntry, ClassEntry outerEntry) {
		this.innerClassesByOuter.put(outerEntry, innerEntry);
		this.outerClassesByInner.putIfAbsent(innerEntry, outerEntry);
	}

	private MethodEntry findAccessMethod(MethodDefEntry method) {

		// we want to find all compiler-added methods that directly call another with no processing

		// skip non-synthetic methods
		if (!method.getAccess().isSynthetic()) {
			return null;
		}

		// get all the methods that we call
		final Collection<MethodEntry> referencedMethods = methodReferences.get(method);

		// is there just one?
		if (referencedMethods.size() != 1) {
			return null;
		}

		return referencedMethods.stream().findFirst().orElse(null);
	}

	private boolean isBridgedMethod(MethodEntry called, MethodEntry access) {
		// Bridged methods will always have the same name as the method they are calling
		// They will also have the same amount of parameters (though equal descriptors cannot be guaranteed)
		if (!called.getName().equals(access.getName()) || called.getDesc().getArgumentDescs().size() != access.getDesc().getArgumentDescs().size()) {
			return false;
		}

		TypeDescriptor accessReturn = access.getDesc().getReturnDesc();
		TypeDescriptor calledReturn = called.getDesc().getReturnDesc();
		if (calledReturn.isVoid() || calledReturn.isPrimitive() || accessReturn.isVoid() || accessReturn.isPrimitive()) {
			return false;
		}

		// Bridged methods will never have the same type as what they are calling
		if (accessReturn.equals(calledReturn)) {
			return false;
		}

		String accessType = accessReturn.toString();

		// If we're casting down from generic type to type-erased Object we're a bridge method
		if (accessType.equals("Ljava/lang/Object;")) {
			return true;
		}

		// Now we need to detect cases where we are being casted down to a higher type bound
		List<ClassEntry> calledAncestry = translationIndex.getAncestry(calledReturn.getTypeEntry());
		return calledAncestry.contains(accessReturn.getTypeEntry());
	}

	public Set<ClassEntry> getObfClassEntries() {
		return this.obfClassEntries;
	}

	public Collection<FieldDefEntry> getObfFieldEntries() {
		return this.fields.values();
	}

	public Collection<FieldDefEntry> getObfFieldEntries(ClassEntry classEntry) {
		return this.fields.get(classEntry);
	}

	public Collection<MethodDefEntry> getObfBehaviorEntries() {
		return this.methods.values();
	}

	public Collection<MethodDefEntry> getObfBehaviorEntries(ClassEntry classEntry) {
		return this.methods.get(classEntry);
	}

	public TranslationIndex getTranslationIndex() {
		return this.translationIndex;
	}

	@Deprecated
	public Access getAccess(Entry<?> entry) {
		AccessFlags flags = getAccessFlags(entry);
		return flags != null ? Access.get(flags) : null;
	}

	public AccessFlags getAccessFlags(Entry<?> entry) {
		return this.access.get(entry);
	}

	public ClassInheritanceTreeNode getClassInheritance(ClassEntry obfClassEntry) {

		// get the root node
		List<String> ancestry = Lists.newArrayList();
		ancestry.add(obfClassEntry.getFullName());
		for (ClassEntry classEntry : this.translationIndex.getAncestry(obfClassEntry)) {
			if (containsObfClass(classEntry)) {
				ancestry.add(classEntry.getFullName());
			}
		}
		ClassInheritanceTreeNode rootNode = new ClassInheritanceTreeNode(ancestry.get(ancestry.size() - 1));

		// expand all children recursively
		rootNode.load(this.translationIndex, true);

		return rootNode;
	}

	public ClassImplementationsTreeNode getClassImplementations(ClassEntry obfClassEntry) {

		// is this even an interface?
		if (isInterface(obfClassEntry.getFullName())) {
			ClassImplementationsTreeNode node = new ClassImplementationsTreeNode(obfClassEntry);
			node.load(this);
			return node;
		}
		return null;
	}

	public MethodInheritanceTreeNode getMethodInheritance(MethodEntry obfMethodEntry) {
		// travel to the ancestor implementation
		LinkedList<ClassEntry> entries = new LinkedList<>();
		entries.add(obfMethodEntry.getParent());

		// TODO: This could be optimized to not go through interfaces repeatedly...

		ClassEntry baseImplementationClassEntry = obfMethodEntry.getParent();

		for (ClassEntry itf : getInterfaces(obfMethodEntry.getParent().getFullName())) {
			MethodEntry itfMethodEntry = entryPool.getMethod(itf, obfMethodEntry.getName(), obfMethodEntry.getDesc().toString());
			if (itfMethodEntry != null && containsObfMethod(itfMethodEntry)) {
				baseImplementationClassEntry = itf;
			}
		}

		for (ClassEntry ancestorClassEntry : this.translationIndex.getAncestry(entries.remove())) {
			MethodEntry ancestorMethodEntry = entryPool.getMethod(ancestorClassEntry, obfMethodEntry.getName(), obfMethodEntry.getDesc().toString());
			if (ancestorMethodEntry != null) {
				if (containsObfMethod(ancestorMethodEntry)) {
					baseImplementationClassEntry = ancestorClassEntry;
				}

				for (ClassEntry itf : getInterfaces(ancestorClassEntry.getFullName())) {
					MethodEntry itfMethodEntry = entryPool.getMethod(itf, obfMethodEntry.getName(), obfMethodEntry.getDesc().toString());
					if (itfMethodEntry != null && containsObfMethod(itfMethodEntry)) {
						baseImplementationClassEntry = itf;
					}
				}
			}
		}

		// make a root node at the base
		MethodEntry methodEntry = entryPool.getMethod(baseImplementationClassEntry, obfMethodEntry.getName(), obfMethodEntry.getDesc().toString());
		MethodInheritanceTreeNode rootNode = new MethodInheritanceTreeNode(
				methodEntry,
				containsObfMethod(methodEntry)
		);

		// expand the full tree
		rootNode.load(this, true);

		return rootNode;
	}

	public List<MethodImplementationsTreeNode> getMethodImplementations(MethodEntry obfMethodEntry) {
		List<MethodEntry> interfaceMethodEntries = Lists.newArrayList();

		// is this method on an interface?
		if (isInterface(obfMethodEntry.getParent().getFullName())) {
			interfaceMethodEntries.add(obfMethodEntry);
		} else {
			// get the interface class
			for (ClassEntry interfaceEntry : getInterfaces(obfMethodEntry.getParent().getFullName())) {

				// is this method defined in this interface?
				MethodEntry methodInterface = entryPool.getMethod(interfaceEntry, obfMethodEntry.getName(), obfMethodEntry.getDesc().toString());
				if (methodInterface != null && containsObfMethod(methodInterface)) {
					interfaceMethodEntries.add(methodInterface);
				}
			}
		}

		List<MethodImplementationsTreeNode> nodes = Lists.newArrayList();
		if (!interfaceMethodEntries.isEmpty()) {
			for (MethodEntry interfaceMethodEntry : interfaceMethodEntries) {
				MethodImplementationsTreeNode node = new MethodImplementationsTreeNode(interfaceMethodEntry);
				node.load(this);
				nodes.add(node);
			}
		}
		return nodes;
	}

	public Set<MethodEntry> getRelatedMethodImplementations(MethodEntry obfMethodEntry) {
		AccessFlags flags = getAccessFlags(obfMethodEntry);
		if (flags == null) {
			throw new IllegalArgumentException("Could not find method " + obfMethodEntry);
		}

		if (flags.isPrivate() || flags.isStatic()) {
			return Collections.singleton(obfMethodEntry);
		}

		Set<MethodEntry> methodEntries = Sets.newHashSet();
		getRelatedMethodImplementations(methodEntries, getMethodInheritance(obfMethodEntry));
		return methodEntries;
	}

	private void getRelatedMethodImplementations(Set<MethodEntry> methodEntries, MethodInheritanceTreeNode node) {
		MethodEntry methodEntry = node.getMethodEntry();
		if (methodEntries.contains(methodEntry)) {
			return;
		}

		if (containsObfMethod(methodEntry)) {
			AccessFlags flags = getAccessFlags(methodEntry);
			if (!flags.isPrivate() && !flags.isStatic()) {
				// collect the entry
				methodEntries.add(methodEntry);
			}
		}

		// look at bridge methods!
		MethodEntry bridgedMethod = getBridgedMethod(methodEntry);
		while (bridgedMethod != null) {
			methodEntries.addAll(getRelatedMethodImplementations(bridgedMethod));
			bridgedMethod = getBridgedMethod(bridgedMethod);
		}

		// look at interface methods too
		for (MethodImplementationsTreeNode implementationsNode : getMethodImplementations(methodEntry)) {
			getRelatedMethodImplementations(methodEntries, implementationsNode);
		}

		// recurse
		for (int i = 0; i < node.getChildCount(); i++) {
			getRelatedMethodImplementations(methodEntries, (MethodInheritanceTreeNode) node.getChildAt(i));
		}
	}

	private void getRelatedMethodImplementations(Set<MethodEntry> methodEntries, MethodImplementationsTreeNode node) {
		MethodEntry methodEntry = node.getMethodEntry();
		if (containsObfMethod(methodEntry)) {
			AccessFlags flags = getAccessFlags(methodEntry);
			if (!flags.isPrivate() && !flags.isStatic()) {
				// collect the entry
				methodEntries.add(methodEntry);
			}
		}

		// look at bridge methods!
		MethodEntry bridgedMethod = getBridgedMethod(methodEntry);
		while (bridgedMethod != null) {
			methodEntries.addAll(getRelatedMethodImplementations(bridgedMethod));
			bridgedMethod = getBridgedMethod(bridgedMethod);
		}

		// recurse
		for (int i = 0; i < node.getChildCount(); i++) {
			getRelatedMethodImplementations(methodEntries, (MethodImplementationsTreeNode) node.getChildAt(i));
		}
	}

	public Collection<EntryReference<FieldEntry, MethodDefEntry>> getFieldReferences(FieldEntry fieldEntry) {
		return this.fieldReferences.get(fieldEntry);
	}

	public Collection<FieldEntry> getReferencedFields(MethodDefEntry methodEntry) {
		// linear search is fast enough for now
		Set<FieldEntry> fieldEntries = Sets.newHashSet();
		for (EntryReference<FieldEntry, MethodDefEntry> reference : this.fieldReferences.values()) {
			if (reference.context == methodEntry) {
				fieldEntries.add(reference.entry);
			}
		}
		return fieldEntries;
	}

	public Collection<EntryReference<ClassEntry, MethodDefEntry>> getMethodsReferencing(ClassEntry classEntry) {
		return this.methodsReferencingClasses.get(classEntry);
	}

	@Deprecated
	public Collection<EntryReference<MethodEntry, MethodDefEntry>> getMethodsReferencing(MethodEntry methodEntry) {
		return getMethodsReferencing(methodEntry, false);
	}

	public Collection<EntryReference<MethodEntry, MethodDefEntry>> getMethodsReferencing(MethodEntry methodEntry, boolean recurse) {
		if (!recurse) {
			return this.methodsReferencing.get(methodEntry);
		}

		List<EntryReference<MethodEntry, MethodDefEntry>> references = new ArrayList<>();
		Set<MethodEntry> methodEntries = getRelatedMethodImplementations(methodEntry);
		for (MethodEntry entry : methodEntries) {
			references.addAll(getMethodsReferencing(entry, false));
		}
		return references;
	}

	public Collection<MethodEntry> getReferencedMethods(MethodDefEntry methodEntry) {
		return this.methodReferences.get(methodEntry);
	}

	public Collection<ClassEntry> getInnerClasses(ClassEntry obfOuterClassEntry) {
		return this.innerClassesByOuter.get(obfOuterClassEntry);
	}

	public ClassEntry getOuterClass(ClassEntry obfInnerClassEntry) {
		return this.outerClassesByInner.get(obfInnerClassEntry);
	}

	public boolean isSyntheticMethod(MethodEntry methodEntry) {
		return this.syntheticMethods.contains(methodEntry);
	}

	public Set<ClassEntry> getInterfaces(String className) {
		ClassEntry classEntry = entryPool.getClass(className);
		Set<ClassEntry> interfaces = new HashSet<>(this.translationIndex.getInterfaces(classEntry));
		for (ClassEntry ancestor : this.translationIndex.getAncestry(classEntry)) {
			interfaces.addAll(this.translationIndex.getInterfaces(ancestor));
		}
		return interfaces;
	}

	public Set<String> getImplementingClasses(String targetInterfaceName) {

		// linear search is fast enough for now
		Set<String> classNames = Sets.newHashSet();
		for (Map.Entry<ClassEntry, ClassEntry> entry : this.translationIndex.getClassInterfaces()) {
			ClassEntry classEntry = entry.getKey();
			ClassEntry interfaceEntry = entry.getValue();
			if (interfaceEntry.getFullName().equals(targetInterfaceName)) {
				String className = classEntry.getFullName();
				classNames.add(className);
				if (isInterface(className)) {
					classNames.addAll(getImplementingClasses(className));
				}

				this.translationIndex.getSubclassNamesRecursively(classNames, classEntry);
			}
		}
		return classNames;
	}

	public boolean isInterface(String className) {
		return this.translationIndex.isInterface(entryPool.getClass(className));
	}

	public boolean containsObfClass(ClassEntry obfClassEntry) {
		return this.obfClassEntries.contains(obfClassEntry);
	}

	public boolean containsObfField(FieldEntry obfFieldEntry) {
		return this.access.containsKey(obfFieldEntry);
	}

	public boolean containsObfMethod(MethodEntry obfMethodEntry) {
		return this.access.containsKey(obfMethodEntry);
	}

	public boolean containsObfVariable(LocalVariableEntry obfVariableEntry) {
		// check the behavior
		return containsObfMethod(obfVariableEntry.getParent());
	}

	public boolean containsObfEntry(Entry<?> obfEntry) {
		if (obfEntry == null) {
			throw new IllegalArgumentException("Cannot check for null entry");
		}

		if (obfEntry instanceof ClassEntry) {
			return containsObfClass((ClassEntry) obfEntry);
		} else if (obfEntry instanceof FieldEntry) {
			return containsObfField((FieldEntry) obfEntry);
		} else if (obfEntry instanceof MethodEntry) {
			return containsObfMethod((MethodEntry) obfEntry);
		} else if (obfEntry instanceof LocalVariableEntry) {
			return containsObfVariable((LocalVariableEntry) obfEntry);
		} else {
			throw new Error("Entry desc not supported: " + obfEntry.getClass().getName());
		}
	}

	public MethodEntry getBridgedMethod(MethodEntry bridgeMethodEntry) {
		return this.bridgedMethods.get(bridgeMethodEntry);
	}

	public List<ClassEntry> getObfClassChain(ClassEntry obfClassEntry) {

		// build class chain in inner-to-outer order
		List<ClassEntry> obfClassChain = Lists.newArrayList(obfClassEntry);
		ClassEntry checkClassEntry = obfClassEntry;
		while (true) {
			ClassEntry obfOuterClassEntry = getOuterClass(checkClassEntry);
			if (obfOuterClassEntry != null) {
				obfClassChain.add(obfOuterClassEntry);
				checkClassEntry = obfOuterClassEntry;
			} else {
				break;
			}
		}

		// switch to outer-to-inner order
		Collections.reverse(obfClassChain);

		return obfClassChain;
	}
}
