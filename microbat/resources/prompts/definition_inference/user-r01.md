## Definition Inference Task

You are a Java expert responsible for analyzing whether a specific target line writes to a given variable that is later read at a usage line.

Write means any assignment or side effect that updates the exact storage location denoted by the variable path on the same object, including indirect mutations of a container’s internal representation (e.g., backing arrays, node entries, bucket arrays, or mapping slots) when those effects follow from the documented API semantics of standard JDK classes. Do not rely on concrete private field names; treat paths like map.map["k"], elementData[i], table[bucket], or similar as abstract aliases for the logical storage of container contents.

Key principles:
- Exact path/slot alignment:
  - The write must affect the same object and the same field/index/key identified by the variable path.
  - For arrays/lists, the index must match; for maps/sets, the key (or category) must match; different indices/keys do not count.
  - If the path uses an internal bucket index (e.g., table[3]) and you cannot deterministically map the operation to that bucket, default to False. If the path uses the logical key (e.g., map["k"]), align by key.
- Aliasing:
  - A write counts only if it targets the same underlying object/array element/map slot as the variable path. Use the provided alias information to confirm object identity. Operations on returned copies or different objects do not write the original.
- Temporal reachability:
  - The location must be reachable via the variable path at the time of the mutation. Assigning a parent reference (obj.field = new T(...)) writes obj.field but does not write obj.field.subField unless subField existed on the already-aliased object before and is mutated in place.
  - Constructor-time initialization of a newly allocated object does not count as a write to a nested path until the object is stored into that path. Treat explicit assignments in constructors/field initializers as writes to the fields they set; do not count JVM default zero-initialization as a write.
- Static vs instance:
  - Only treat writes to static/class fields as writes if the Variable itself denotes that static field and the usage reads that same static field. Instance operations do not count as writes to unrelated static fields.
- Method-body and API semantics:
  - If the target line calls a method and its body is provided, inspect it: only count a write if it assigns/mutates the exact target path (or its logical container slot for the aligned key/index).
  - If the body is not provided, use standard JDK API semantics:
    - Mutators write: Map.put/putIfAbsent/replace/compute*/merge/remove/clear; Collection.add/addAll/remove/removeAll/clear; List.set; Queue.offer/poll/add/remove; Arrays.fill; array index stores; AtomicStampedReference.set/compareAndSet/weakCompareAndSet; AtomicLong.set/addAndGet and similar.
    - Read-only: get/contains*/size/isEmpty/peek/element/get(int)/iterator/listIterator/for-each; toString/String.valueOf/concatenation/StringBuilder.append; PrintStream.print/println/logging; stream operations without side effects.
  - For conditionally writing mutators (e.g., replace, compareAndSet), apply may-write semantics: answer True if there exists any normal execution path where the method could update the target location, unless the input definitively proves the condition cannot hold.
- Container content vs metadata:
  - Treat container content updates as writes to their internal storage slots (e.g., elementData[i], table[key], node.value) that corresponding reads consult (e.g., get/contains/iteration). Do not count unrelated metadata unless it is part of the variable path.
  - Key/index matching is required: writing one key does not write another key’s slot; containsKey(arg) reads the presence of arg specifically.
- Bucket indices and internal nodes:
  - For hash-based structures, only claim a write to a specific bucket index or node subfield if the operation deterministically targets that bucket/node at the time of mutation. Do not infer writes to a different bucket/node based on later snapshots.
  - Special case: HashMap (and subclasses like Provider) put may lazily initialize or resize the internal table; this writes table.length. Therefore, a put can write map.table.length even if no entries are yet present.
- Copies and views:
  - Operations on copies (e.g., Arrays.copyOf, methods documented to return a new object) do not write back to the original unless explicitly stored into the aliased field.
- Exclusions:
  - String concatenation, StringBuilder usage, getters (get*/is*/to*), toString, printing/logging are read-only with respect to the target object, unless the invoked method is a known mutator of the target field/path.
  - Do not attribute writes to the target object based on mutations to other objects involved in the expression (e.g., System.out).
- Conservative defaults:
  - Use provided method bodies or well-known JDK semantics to establish side effects. Do not assume hidden or undocumented mutations. If aliasing or slot alignment is not satisfied, answer False.
  - However, for recognized mutators that can update the aligned slot (same key/index) or known internal storage they manage (e.g., backing array growth, map table initialization), answer True even if the update is conditional and its success cannot be proven from the snapshot.

Decision checklist before answering:
1) Identify the precise variable path (object.field/index/key). Normalize whether it denotes:
   - a direct field, or
   - a container’s logical slot (e.g., map["k"], elementData[i], table[key/bucket]).
2) Confirm aliasing: ensure the target line’s receiver is the same object as the root of the variable path per alias info. Exclude effects on other objects or copies.
3) Determine what the usage line reads: verify it consults the same path/slot (same key/index/field). If it reads a different slot, answer False.
4) Analyze the target line:
   - If it directly assigns the exact field/slot, answer True.
   - If it calls a method/constructor:
     a) If bodies are provided, verify an explicit assignment/mutation to the exact field/slot (or its logical container slot for the aligned key/index) on a reachable path; if yes, True.
     b) If bodies are not provided, apply standard JDK semantics:
        - Mutators listed above write. Ensure key/index alignment. For conditionals/CAS, treat as may-write: True if it can write.
        - Read-only methods do not write: False.
5) Temporal reachability: ensure the location existed on the path at mutation time. Parent-reference assignment alone is not a write to nested fields; constructor-time initialization counts only after the object is aliased into the path.
6) Special classes guidance:
   - EnumMap.put(k,v) writes the mapping for k; containsValue/get/iteration read that mapping -> treat as a write to the container’s internal mapping.
   - LinkedList get(int), listIterator(int), and next() are read-only; they do not modify nodes or the first element.
   - ArrayList add(i,x)/add(x)/set(i,x) write elementData at the targeted index; get(i) reads the same.
   - HashMap/LinkedHashMap put/replace/compute*/merge/remove/clear write internal table entries for the targeted key; get/containsKey reads the same key’s slot. HashMap.put may allocate/resize the table, writing table.length.
   - AtomicStampedReference set/compareAndSet/weakCompareAndSet can update the internal Pair (reference and stamp). Treat the stamp/reference fields as potentially written by these calls (may-write semantics).
7) Static fields: only count writes if the variable explicitly denotes the static field and the usage reads it.
8) Default: if any of aliasing, path/slot alignment, or mutator semantics cannot be established, answer False.

Response format:
- Output only one line: Answer: <T> if the target line writes (or may write) the specified variable according to the rules above; Answer: <F> otherwise.
- Do not include any additional text or thoughts.

Examples:
- EnumMap example (positive):
  - Target: map.put(Color.RED, "apple")
  - Variable: map.map["RED"] (logical mapping slot)
  - Usage: map.containsValue("apple")
  - Answer: <T> (put writes the mapping; containsValue reads the container’s contents)
- Logging/printing (negative):
  - Target: System.out.println("... " + list.get(2))
  - Variable: list.first
  - Usage: list.get(0)
  - Answer: <F> (only PrintStream/StringBuilder are written; list is read)
- LinkedBlockingQueue array-path (negative):
  - Target: q.add(x)
  - Variable: q.queue.queue[0] (array-like path not used by LinkedBlockingQueue)
  - Usage: q.peek()
  - Answer: <F> (LinkedBlockingQueue uses linked nodes, not an array field at that path)
- AtomicStampedReference (positive, may-write):
  - Target: asr.weakCompareAndSet(expRef, newRef, expStamp, newStamp)
  - Variable: asr.pair.stamp
  - Usage: asr.getStamp()
  - Answer: <T> (the operation can update the stamp on a feasible path)

### Instructions

1. You will receive a target line of code.
2. You will also be given a variable name and the line of code where this variable is used.
3. You will also be given a read variable line that reads the variable.
4. Your objective is to determine whether the target line writes to the specified variable, using the rules and checklist above.

### Response Format
- Answer: <T> if the target line writes (or may write on a feasible path) to the variable.
- Answer: <F> if it does not.

### Examples:

${example}

### Your Turn:

Now, please analyze the following input according to the provided format.
In your response, return <T> for true and <F> for false.

### Question:
**Target Line:**
`${targetLine}`

${functionCalls}

**Variables Involved:**

${variables}

we know that `${rootVariable}` has the following structure and value:
${abstractVariableInfo}
But we don't know which step during the execution modified the value.

**Usage Line:**
`${usageLine}`

We know that the target line and the usage line share the following alias information:
${aliasInfo}


`${rootVariable}` has a field `${cascadeFieldName}`, does the code `${targetLine}` directly or indirectly write field `${cascadeFieldName}`?
In your response, you should first use <thought></thought> tags to express your thought process, and finally answer with "Answer: <T>" or "Answer: <F>".