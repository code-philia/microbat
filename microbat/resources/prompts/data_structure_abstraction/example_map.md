**Input:**

The type of the variable is:
`java.util.concurrent.ConcurrentHashMap`

The toString value of the variable is:
`{key1=10, key2=20, key5=50, key3=30, key4=40, key=42}`

The concrete value of the variable is:

```json
{"map|java.util.concurrent.ConcurrentHashMap": {"cellsBusy|int": "0", "transferIndex|int": "0", "sizeCtl|int": "12", "baseCount|long": "6", "table|java.util.concurrent.ConcurrentHashMap$Node[]": ["key1=10", "key2=20", "null", "null", "key5=50", "null", "key3=30", "key4=40", "null", "null", "null", "null", "key=42"], "nextTable|null": null, "counterCells|null": null}}
```

**Example Output:**

```json
{
  "ConcurrentHashMap": {
    "metadata": {
      "cellsBusy": 0,
      "transferIndex": 0,
      "sizeCtl": 12,
      "baseCount": 6
    },
    "table": {
      "key1": 10,
      "key2": 20,
      "key5": 50,
      "key3": 30,
      "key4": 40,
      "key": 42
    },
    "nonExists": "dummy"
  }
}
```