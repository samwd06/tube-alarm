# Notebooks (data exploration)

Notebooks that explore the data saved by the backend. They read straight from the `tube_alarm` database, so run `backend/load_routes.py` (and `backend/log_status.py` for status data) first.

- `explore_network.ipynb` looks at stations per line, the distance between neighbouring stations and the first live status snapshots.

## Setup

The notebooks have their own virtual environment, separate from the backend:

```
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

Then open a notebook in VS Code and pick `notebooks/venv` as the kernel.
