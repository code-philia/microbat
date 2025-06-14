
# Background
When executing Java code involving libraries, understanding the internal structure of objects is critical for debugging. Given the "toString" value of a value, your task is to **analyze a Java object** and **generate a expanded JSON representation of internal values** that captures its internal fields relevant to debugging.

During generated JSON representation, you will be given a focal vairable path, e.g. `list.elementData[0].name`, and for objects fields, you should focus on the focal variables and their values, and you can omit the rest fields. But DO NOTICE that, the output should be a JSON object, strictly following the JSON grammar, to omit a field in a JSON object, DO NOT use "...", but just delete the field to keep the JSON valid. For list or array, DO NOT omit any element in LIST. If the focal variable is "#all_fields#", you should ignore the focal variable and return a complete JSON object with the all fields.

The expanded JSON representation should:

- Strictly follow the format `"var_name|var_type": var_value`.
- Reflect inferred values and types for all fields used or affected in the operation.
- Be rooted at the top-level variable (e.g., `set`).
- Wrap your output within a json code block (i.e., ```json ... ```). Do not include any comments inside the json code block.
- If needed, use <thought>xxxx</thought> tags to express your thought process first. The thoughts should be as short as possible.
- If the focal variable contains a deep recursive structure (e.g., a tree or a linked list), you should only expand the first level of the structure. For example, if the focal variable is a tree, you should only expand the root node and its immediate children, but not the entire tree.

# Example

**Focal Variable toString Value:**
`${exampleValue}`

**Focal Variable Type Name:**
`${exampleType}`

**Related Class Structures:**
${exampleClassStructures}

**Focal Variable Path:**
`${exampleFocalPath}`

**Expected Output:**
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

**Focal Variable Path:**
`${focalPath}`

**Related Class Structures:**
${classStructures}

**Line of Code Containing the Variable:**
```java
${code}
```

**Output:**