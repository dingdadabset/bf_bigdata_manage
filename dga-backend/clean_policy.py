import json

with open('policy_2.json', 'r') as f:
    data = json.load(f)

items = data.get('policyItems', [])
new_items = []

for item in items:
    users = item.get('users', [])
    if 'dingquan5' in users:
        print("Removing dingquan5 from policy item")
        # If there are other users, keep them (but here it seems dingquan5 is alone in that item)
        # The structure is specific: users=["dingquan5"]
        # If users has multiple, remove only dingquan5
        users.remove('dingquan5')
        if users or item.get('groups'): # Keep item if it still has users or groups
            new_items.append(item)
    else:
        new_items.append(item)

data['policyItems'] = new_items

with open('policy_2_clean.json', 'w') as f:
    json.dump(data, f, indent=4)
