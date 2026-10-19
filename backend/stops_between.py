import os
import requests
from dotenv import load_dotenv

load_dotenv("../.env")
api_key = os.getenv("TFL_APP_KEY")


def get_stations():
    url = "https://api.tfl.gov.uk/Line/victoria/Route/Sequence/outbound"
    response = requests.get(url, params={"app_key": api_key})
    response.raise_for_status()
    stops = response.json()["stopPointSequences"][0]["stopPoint"]
    # Drop " Underground Station" so names are easy to type
    return [stop["name"].replace(" Underground Station", "") for stop in stops]


def stops_between(start, end):
    names = get_stations()
    a = names.index(start)
    b = names.index(end)
    if a <= b:
        return names[a:b + 1]
    # Travelling the other way, so reverse the order
    return names[b:a + 1][::-1]


if __name__ == "__main__":
    for name in stops_between("Stockwell", "Green Park"):
        print(name)
