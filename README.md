## Downloading Required Data

### OSM Data
OSM data can be downloaded from the following source:
[Geofabrik Europe](https://download.geofabrik.de/europe.html) (Use "Ireland and Northern Ireland" for the required region.)

### GTFS Data
GTFS data can be downloaded from the following source:
[TransitFeeds - Transport for Ireland](https://transitfeeds.com/p/transport-for-ireland/782)

After downloading the GTFS data, extract the contents before use.

## Where to Place the Data
Place the downloaded and extracted GTFS data inside the `data/gtfs/` directory within the project structure:

```
route-calculation-service/
└── src/
    └── main/
        └── resources/
            ├── application.properties
            ├── gtfs/
                ├── agency.txt
                ├── calendar.txt
                ├── calendar_dates.txt
                ├── routes.txt
                ├── shapes.txt
                ├── stop_times.txt
                ├── stops.txt
                ├── transfers.txt
                └── trips.txt
            ├── data/
                └── ireland-and-northern-ireland-latest.osm.pbf
```

Ensure that the GTFS data is uncompressed before use. The `data/gtfs/` directory should contain `.txt` files extracted from the GTFS ZIP archive.

