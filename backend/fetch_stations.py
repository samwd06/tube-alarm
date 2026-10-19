import os
import requests
from dotenv import load_dotenv

# Read the API key from the .env file in the main project folder
load_dotenv("../.env")
api_key = os.getenv("TFL_APP_KEY")

url = "https://api.tfl.gov.uk/Line/victoria/Route/Sequence/outbound"
response = requests.get(url, params={"app_key": api_key})
response.raise_for_status()  # stop with an error if the request failed

data = response.json()

# The stations in travel order are inside the first route sequence
stations = data["stopPointSequences"][0]["stopPoint"]

for station in stations:
    print(station["name"], station["lat"], station["lon"])