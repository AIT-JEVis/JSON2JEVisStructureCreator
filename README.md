# JSON2JEVisStructureCreator
Parse a JSON-file and create the object-structure in JEVis3

## Commands
The following commands can be given (default is `0`). The commands are given in the `"id"` field of the JSON-file
```
private interface OPERATIONS {
        final long CREATE = 0; // or update
        final long IGNORE = -1;
        final long DELETE = -2;
        final long DELETE_RECURSIVE = -3;
        final long RENAME = -4;
        final long DELETE_OLD_SAMPLES = -5;
        final long DELETE_OLD_SAMPLES_RECURSIVE = -6;
    }
```

Note: the command `RENAME` is not implemented. Pull requests are quite welcome.


### Create and delete
For example, to create an Organization "Desigo" in the Organization Directory write the following JSON-file
```
{
    "id": "-1",
    "name": "Organization Directory",
    "jevisclass": "Organization Directory",
    "attributes": [],
    "children": [
        {
            "id": "-3",
            "name": "Desigo",
            "jevisclass": "Organization"
        },
        {
            "name": "Desigo",
            "jevisclass": "Organization",
            "attributes": [
                {
                    "name": "Address",
                    "lastvalue": "Some Random Adress"
                },
                {
                    "name": "Members",
                    "lastvalue": "Me"
                }
            ],
        }
    ]
}
```

This file first sets the root to be the "Organization Directory". the `id` of `-1` tells the creator to just find the described object and don't do anything to the object. This can be used to specify the tree structure to create the new objects under.

Then any old organization named "Desigo" will be deleted (`id` of `-2` or `-3`).

Afterwards create a new Organization (no id specified, defaults to `0`, which is the command `CREATE`).

### Delete old samples
The commands `-5` and `-6` instruct the creator to delete all samples except the latest one. `-6` also recureses deeper into the tree.

For example to delete all old drivers one can use the following JSON-description (taken from [delete_old_driver_jars.json](delete_old_driver_jars.json)).
```
{
    "id": "-1",
    "name": "JEDataCollector #1",
    "jevisclass": "JEDataCollector",
    "children": [
        {
            "id": "-6",
            "name": "Data Source Driver Directory",
            "jevisclass": "Data Source Driver Directory"
        },
        {
            "id": "-6",
            "name": "Parser Driver Directory",
            "jevisclass": "Parser Driver Directory"
        }
    ]
}
```


## Tags
There are some tags which are treated special by the creator.

### Reference
The tag `$(REF)refid` tries to replace a given `refid` with an id previously defined in the JSON file.

When parsing the json file all values in the field `id` greater than `0` are treated as references. While creating a described object with an `id` greater than `0`, then the creator saves the created jevis-internal id. A described object later in the file can then use the `$(REF)` tag to replace the given id with the previously created jevis-internal id.

For example first describe an object with `"id": "1"`. Later in the file use it in an attribute as lastvalue, i.e. `"lastvalue": "$(REF)1"`.

See [DesigoStructure.json](DesigoStructure.json) for more examples.

### Upload a file
The tag `$(FILE)filepath` uploads the file given by `filepath`.

The path is interpreted to be relative to the JSON-file.

See [MySQLDriverObjects.json](https://github.com/AIT-JEVis/MySQL-Driver/blob/master/MySQLDriverObjects.json) for an example how to upload a MySQL driver.
