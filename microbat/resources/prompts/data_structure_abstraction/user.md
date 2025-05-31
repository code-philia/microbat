# Semantic Abstraction of Java Data Structures

## Background

You are an expert in Java runtime structures and your goal is to semantically extract and abstract the meaning of complex serialized Java objects. The input will be a Java object (or nested structure) serialized in a pseudo-JSON format, where class/type names are annotated with the field (e.g., `"map|java.util.HashMap"`). Your task is to return a **abstracted** JSON representation capturing the essential semantics.

Specifically:

- If a structure contains metadata or configuration fields (like counters, thresholds), group them under `"metadata"`.
- If a structure stores key-value data (like `Map`, `ConcurrentHashMap`, or table arrays with `key=value` strings), extract those into a clean dictionary format.
- If a structure stores set values (like `Set`), extract the unique values into a list named `"set"`.
- If a structure contains a queue or stack, represent it as a list named `"queue"` or `"stack"`.
- For map or set values, you should include an extra `nonExists` key with "dummy" value to indicate any non-existing values.
- Nulls, placeholders like `"PRESENT|java.lang.Object"`, or internal implementation-specific artifacts can be omitted unless meaningful.
- Type annotations (e.g., `|java.util.HashMap`) should be removed from keys in the output.
- You should combine the toString value with the concrete value to generate the output.
- Make sure the toString value is grouped under appropriate field.
- If there are inconsistent between toString values and concrete values, you should use the toString value as the main reference.
- Your think process should be purely textual, and contains no code blocks or json block or markdown formatting.
- Wrap your output in a json code block (i.e., ```json ```) and do not include any comment inside json code block.
- Beware of the output json, do not miss any `"{"` or `"}"` or `","` or `":"` in the output.
- Your json output should be complete and valid, do not leave any incomplete structures, do not use `"..."` to represent incomplete structures.
- For some deep recursive structures (e.g., tree or linked list), the concrete value only contains the first level of the structure. You should try to generate a comprehensive output based on both the toString value and the concrete value. For example, if the target is a linked list, you should not only write the first and the last node, but also the nodes in between in an abstracted way.

---

In your thinking process, go through the following steps and think step by step:
1. Purpose over structure
Focus on what the object represents rather than how it is implemented. This helps decide whether a field is metadata, data, or structural.

2. Prioritize runtime truth
Always give precedence to the toString value when it conflicts with internal fields. Treat it as the authoritative snapshot.

3. Resolve ambiguity
If multiple internal fields could hold the same semantic value (e.g., both table and entrySet in a map), synthesize them into a single, clean abstraction based on the toString.

4. Flatten without oversimplifying
Preserve relevant structure (e.g., map entries, queues, stacks) without over-nesting. Avoid copying deep internals unless necessary.

5. Generalize patterns
When a concrete value hints at repetition (e.g., multiple similar entries or nodes), abstract into representative form even if all instances aren't explicitly listed.

6. Omit irrelevant technical noise
Only include details that affect behavior or understanding. Skip over internal helpers, marker flags, or default capacities unless they convey something semantically useful.

7. Stay concise and coherent
Avoid overgeneration. Reflect only what matters to understanding the high-level state and behavior.
## Example

${example}

---

### Question

**Input:**

The type of the variable is:
`${typeName}`

The toString value of the variable is:
`${toStringValue}`

The concrete value of the variable is:

```json
${concreteValueJson}
```

*Only include the json block in the final answer, never include the json code block in the think process.*
*Make sure your json output is complete and valid*
Strictly Follow below output format:
<think>{think process}</think>
Answer: 
```json
{jsonOutput}
```
