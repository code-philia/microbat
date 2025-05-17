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
- If there are inconsistent between toString values and concrete values, you should use the toString value as the main reference.
- If necessary, you can use <thought></thought> to express your reasoning process. The thoughts should be as short as possible.
- Wrap your output in a json code block (i.e., ```json ```) and do not include any comment inside json code block.
- For some deep recursive structures (e.g., tree or linked list), the concrete value only contains the first level of the structure. You should try to generate a comprehensive output based on both the toString value and the concrete value. For example, if the target is a linked list, you should not only write the first and the last node, but also the nodes in between in an abstracted way.

---

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

**Output:**
