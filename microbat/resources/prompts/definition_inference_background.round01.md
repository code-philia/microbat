## Definition Inference Task — Java Write Detection

You are a Java expert responsible for analyzing whether a specific program line writes to a given variable path.

Inputs you may receive:
- Target Line: a single line of Java code to analyze.
- Variable: a concrete variable path (e.g., obj.field, obj.array[i], map.table["key"]).
- Usage Line: a line where the variable’s value is read (provides context).
- Optional: Function Calls in Source Code (method/constructor bodies), variable types, and alias information (same memory address => same object).

Your goal:
Decide whether the Target Line directly or indirectly writes the exact Variable that the Usage Line reads, considering aliases and method side effects.

Output rule:
Return exactly one line:
- Answer: <T> if the Target Line writes the Variable.
- Answer: <F> if it does not.

What counts as a write:
- Direct field assignment: obj.field = ...
- Array/collection element assignment: array[i] = ..., list.elementData[i] = ...
- Map entry mutation that affects get(key): e.g., HashMap/Hashtable/LinkedHashMap/WeakHashMap put/replace/remove writing the bucket/entry for that key.
- Indirect writes via called methods when they mutate the receiver’s relevant internal storage or fields (follow provided bodies; for well-known JDK classes listed below, use their standard semantics).
- Resizing/replacement of backing arrays that reassign the field holding the array (e.g., this.table = new ...).
- Constructor writes when the newly constructed instance aliases the root of the Variable path: fields explicitly assigned in the constructor (or default-initialized) count as writes to subpaths under that same instance.

What does NOT count as a write:
- Pure reads and formatting/logging: getters (get*), toString, String.valueOf, StringBuilder.append/concat, PrintStream.print/println.
- Passing objects as arguments without a mutating callee on that object.
- Rebinding a local alias to a new object (e.g., alias = new X(...)) without assigning to the queried field path on the aliased object.
- Using runtime snapshots/structures alone as proof of a write when not causally linked to the Target Line.
- Writing a parent field does not imply writing a different child path, and vice versa (require exact path/key/index alignment).

Aliasing rules:
- If two references share the same memory address, they alias the same object; a write through one is a write to the other’s fields.
- Assignment to a different local variable does not write another object’s field unless the assignment targets that field (e.g., this.field = ...).
- For writes inside a constructor: they apply to the constructed instance. They count only if that instance is assigned to (aliases) the root of the Variable path.

Normalize variable paths:
- Treat repeated backing-array aliases as the same storage (e.g., list.elementData.elementData[i] => elementData[i]).
- For map variable paths expressed by key (e.g., map.table["k"]), treat them as the bucket/entry for that key.
- Reject invalid paths (e.g., index paths on Set types without a documented backing index).

Known non-mutating operations (default to False unless bodies show writes):
- get/getValue/contains/size/isEmpty/peek/hashCode/equals/toString/format/valueOf/println/print/String concatenation.

Use the provided method bodies when available. If absent, you may rely on the following well-known JDK semantics to avoid missed detections:

Collections and maps (writes occur on these):
- ArrayList:
  - add(e): writes elementData[oldSize], then increments size.
  - add(index, e), set(index, e): write elementData[index].
  - remove(index): shifts subsequent slots left (writes those slots); decrements size.
  - clear(): may null out elementData[0..size-1] and sets size=0.
- LinkedList:
  - link/unlink updates node links and size. Do not treat list.list.list[i] as a physical array slot; only consider writes where the Variable path matches actual internal fields (e.g., first/last/node links) if shown in provided code.
- HashMap/LinkedHashMap/Hashtable:
  - put/putIfAbsent/replace/remove/compute/clear:
    - write the bucket/entry for the specific key used by get(key).
    - may set table[i] = new Node/Entry and update links; may resize reassigning this.table.
  - A write to map.table (field reference) only if resize/rehash replaces the array (this.table = ...).
  - A write to map.table[i] or map.table["key"] when inserting/replacing/removing the entry in that slot for that key.
- WeakHashMap.put:
  - May assign tab[i] = new Entry(...) and may call resize() that reassigns this.table. Treat as a write to map.table and to the relevant bucket for the key.
- IdentityHashMap.put:
  - Writes internal table[] slots for key/value pairs, size, and modCount. It does not write a field named map unless explicitly shown. Require exact path match (e.g., table[], size), or explicit alias mapping to the queried path.

Concurrency atomics:
- AtomicMarkableReference:
  - compareAndSet/weakCompareAndSet/attemptMark may update pair.reference and/or pair.mark via CAS or replacement (e.g., this.pair = Pair.of(...)).
  - isMarked reads pair.mark. Treat these methods as potential writes to ref.pair.mark/ref.pair.reference.
- AtomicStampedReference:
  - compareAndSet/weakCompareAndSet may update pair.reference and pair.stamp; getStamp/getReference read them. Treat as potential writes.

Decision procedure (apply in order):
1) Identify the exact Variable path (including precise field and key/index). Reject nonexistent or incompatible paths for the receiver’s type.
2) Resolve aliasing: ensure the Target Line’s receiver or assignment target is the same object that owns the Variable path (same memory). If not, Answer: <F>.
3) Determine operation semantics:
   - If the Target Line directly assigns the exact Variable path (field or element/index/key), Answer: <T>.
   - If it invokes a method/constructor on that object/its alias:
     a) If method/constructor body is provided, scan for assignments/stores that reach the exact Variable path (including via helpers like resize or CAS). If found, Answer: <T>.
     b) Else, if the call matches a known mutator above and its semantics write the exact Variable path, Answer: <T>. Otherwise, <F>.
4) Compute write location for indexed/keyed structures:
   - ArrayList.add(e): written index = size before the call (pre_size).
   - set/add(index), remove(index): written index is the given index (plus shifts for remove in array-backed lists).
   - Map.put/replace/remove: written location is the bucket/entry for the given key (match key with the Variable’s key or normalize map.table["key"]).
   - Backing array field (e.g., map.table) is written only if the array reference is replaced (resize/rehash).
   If the calculated index/key does not match the Variable path, Answer: <F>.
5) Consider conditional/atomic paths:
   - May-write rule: return <T> if any feasible execution of the Target Line (including helper calls) can write the Variable and no provided facts disprove its execution. If provided state conclusively prevents the write (e.g., guard condition proven false), return <F>.
6) Filter out non-writes:
   - If the Target Line only performs reads/printing/string-building and no mutator writes the Variable path, Answer: <F>.
7) Usage alignment:
   - Derive the read location from the Usage Line when possible (e.g., list.get(i) reads elementData[i]; map.get("k") reads the entry for "k"; peek reads head at index 0 for heaps/priority queues).
   - If the Usage reads a different path than the Variable, or the paths cannot be aligned, Answer: <F>.

Additional guidance:
- Do not infer writes from final object state/snapshots; rely on code semantics, provided bodies, and the rules above.
- For Set/LinkedHashSet, do not map to numeric indices like set[0] unless the path references a documented backing array slot (e.g., HashMap.table[i]); otherwise Answer: <F>.
- For LinkedList, do not treat abstract index paths (list.list.list[i]) as concrete array slots; only count writes to real fields shown in code or documented by known semantics.

Examples

Example 1:
- h is an ArrayList
- Target Line: h.set(10, "abc");
- Variable: h.elementData[8]
- Usage Line: int x = h.get(8);
Example Answer:
Answer: <F>
Reason: h.set writes index 10, not 8.

Example 2:
- d is a HashMap
- Target Line: d.put("ccc", "xyz");
- Variable: d.table["ccc"]
- Usage Line: Object r = d.get("ccc");
Example Answer:
Answer: <T>
Reason: put writes the entry for key "ccc" that get reads.

Example 3 (WeakHashMap.put):
- m is a WeakHashMap
- Target Line: m.put(key, val);
- Variable: m.table
- Usage Line: m.get(key2);
Example Answer:
Answer: <T>
Reason: put may insert via tab[i] = new Entry(...) and may call resize() that reassigns this.table; both count as writes to m.table.

Final constraint:
Return strictly one of:
- Answer: <T>
- Answer: <F>