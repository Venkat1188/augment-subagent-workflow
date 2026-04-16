import yaml
import os
import sys

def validate_dapr_components(directory):
    """
    Validates Dapr component YAML files against the required schema.
    Specifically checks for apiVersion, kind, metadata, and spec.
    """
    required_fields = ["apiVersion", "kind", "metadata", "spec"]
    results = []
    has_errors = False

    if not os.path.exists(directory):
        print(f"Error: Directory '{directory}' not found.")
        sys.exit(1)

    print(f"--- Dapr Configuration Validation Report ---")

    for filename in os.listdir(directory):
        if filename.endswith(".yaml") or filename.endswith(".yml"):
            path = os.path.join(directory, filename)
            try:
                with open(path, 'r') as f:
                    # Load all documents in case of multi-doc YAMLs
                    docs = yaml.safe_load_all(f)
                    for i, data in enumerate(docs):
                        if not data: continue

                        # Only validate if kind is 'Component'
                        if data.get("kind") == "Component":
                            missing = [field for field in required_fields if field not in data]
                            if not missing:
                                print(f"✅ {filename} (Doc {i}): Valid Dapr Component")
                            else:
                                print(f"❌ {filename} (Doc {i}): Missing fields: {', '.join(missing)}")
                                has_errors = True
                        else:
                            print(f"ℹ️ {filename}: Found {data.get('kind')}, skipping component validation.")
            except Exception as e:
                print(f"❌ {filename}: Critical parsing error - {str(e)}")
                has_errors = True

    if has_errors:
        print("\nValidation FAILED. Please fix the errors above.")
        sys.exit(1)
    else:
        print("\nAll Dapr components are VALID.")
        sys.exit(0)

if __name__ == "__main__":
    target_dir = sys.argv[1] if len(sys.argv) > 1 else "./components"
    validate_dapr_components(target_dir)
