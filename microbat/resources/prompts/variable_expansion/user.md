
# Background
When executing Java code involving libraries, understanding the internal structure of objects is critical for debugging. Given the "toString" value of a value, your task is to **analyze a Java object** and **generate a expanded JSON representation of internal values** that captures its internal fields relevant to debugging.

The expanded JSON representation should:

- Strictly follow the format `"var_name|var_type": var_value`.
- Reflect inferred values and types for all fields used or affected in the operation.
- Be rooted at the top-level variable (e.g., `set`).
- ensure toString value is included and associated with the appropriate internal field.
- Wrap your output within a json code block (i.e., ```json ... ```). Do not include any comments inside the json code block.
- Beware of the output json, do not miss any `"{"` or `"}"` or `","` or `":"` in the output.
- Your json output should be complete and valid, do not leave any incomplete structures, do not use `"..."` to represent incomplete structures.
- Your think process should be purely textual, and contains no code blocks or json block or markdown formatting.
- If the focal variable contains a deep recursive structure (e.g., a tree or a linked list), you should only expand the first level of the structure. For example, if the focal variable is a tree, you should only expand the root node and its immediate children, but not the entire tree.

In your thinking process, go through the following steps and think step by step:
1. Interpret the toString value – What does it reveal about the variable’s contents?

2. Match fields – Which fields from the related class structure are likely responsible for producing or holding this toString value?

3. Use the line of code context – Which fields are accessed or affected in the line of code? (e.g., a get() or isEmpty() call hints at internal state checks.)

4. Select meaningful fields only – Only include fields that contribute to the behavior or output of the variable. Ignore unrelated structural fields.

5. Infer types and values if missing – For fields not shown in the toString, but required for correct behavior (e.g., size in a map), assign inferred or placeholder values.

6. Respect structure depth – If the object is recursive or complex, only expand the first level, not full internal chains.
# Example

**Focal Variable toString Value:**
`${exampleValue}`

**Focal Variable Type Name:**
`${exampleType}`

**Related Class Structures:**
${exampleClassStructures}

**Output:**
```json
${exampleExpanded}
```

## Task

**Focal Variable Name:**
`${name}`

**Focal Variable Type Name:**
`${type}`

**Focal Variable toString Value:**
`${value}`

**Related Class Structures:**
${classStructures}

**Line of Code Containing the Variable:**
```java
${code}
```
*Only include your json block in the final answer, never include your json block in the think process.*
*Make sure your json output is complete and valid*
Strictly Follow below output format:
<think>{think process}</think>
Answer: 
```json
{jsonOutput}
```