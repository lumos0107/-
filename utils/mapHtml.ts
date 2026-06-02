import { RoutePoint } from './api'

export function getStaticMapHtml(token: string, points: RoutePoint[]): string {
  const coords = points.map((p) => [p.longitude, p.latitude])

  return `<!DOCTYPE html>
<html>
<head>
  <meta charset='utf-8' />
  <meta name='viewport' content='width=device-width, initial-scale=1, maximum-scale=1' />
  <link href='https://api.mapbox.com/mapbox-gl-js/v3.3.0/mapbox-gl.css' rel='stylesheet' />
  <script src='https://api.mapbox.com/mapbox-gl-js/v3.3.0/mapbox-gl.js'></script>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body { width: 100vw; height: 100vh; overflow: hidden; }
    #map { width: 100%; height: 100%; }
    .mapboxgl-ctrl-logo, .mapboxgl-ctrl-attrib { display: none !important; }
  </style>
</head>
<body>
  <div id='map'></div>
  <script>
    mapboxgl.accessToken = '${token}';
    const coords = ${JSON.stringify(coords)};
    const map = new mapboxgl.Map({
      container: 'map',
      style: 'mapbox://styles/mapbox/streets-v12',
      center: coords[Math.floor(coords.length / 2)],
      zoom: 14,
    });
    map.on('load', () => {
      map.addSource('route', {
        type: 'geojson',
        data: { type: 'Feature', geometry: { type: 'LineString', coordinates: coords } },
      });
      map.addLayer({
        id: 'route', type: 'line', source: 'route',
        layout: { 'line-join': 'round', 'line-cap': 'round' },
        paint: { 'line-color': '#2563eb', 'line-width': 3 },
      });
      const bounds = coords.reduce(
        (b, c) => b.extend(c),
        new mapboxgl.LngLatBounds(coords[0], coords[0])
      );
      map.fitBounds(bounds, { padding: 50, maxZoom: 16 });
      new mapboxgl.Marker({ color: '#059669' }).setLngLat(coords[0]).addTo(map);
      new mapboxgl.Marker({ color: '#dc2626' }).setLngLat(coords[coords.length - 1]).addTo(map);
    });
  </script>
</body>
</html>`
}

export function getRunningMapHtml(token: string, points: RoutePoint[]): string {
  const coords = points.map((p) => [p.longitude, p.latitude])

  return `<!DOCTYPE html>
<html>
<head>
  <meta charset='utf-8' />
  <meta name='viewport' content='width=device-width, initial-scale=1, maximum-scale=1' />
  <link href='https://api.mapbox.com/mapbox-gl-js/v3.3.0/mapbox-gl.css' rel='stylesheet' />
  <script src='https://api.mapbox.com/mapbox-gl-js/v3.3.0/mapbox-gl.js'></script>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body { width: 100vw; height: 100vh; overflow: hidden; }
    #map { width: 100%; height: 100%; }
    .mapboxgl-ctrl-logo, .mapboxgl-ctrl-attrib { display: none !important; }
  </style>
</head>
<body>
  <div id='map'></div>
  <script>
    mapboxgl.accessToken = '${token}';
    const routeCoords = ${JSON.stringify(coords)};
    let posMarker = null;
    let trackedCoords = [];

    function lineGeoJSON(c) {
      const safe = c.length >= 2 ? c : [routeCoords[0], routeCoords[0]];
      return { type: 'Feature', geometry: { type: 'LineString', coordinates: safe } };
    }

    const map = new mapboxgl.Map({
      container: 'map',
      style: 'mapbox://styles/mapbox/streets-v12',
      center: routeCoords[0],
      zoom: 15,
    });

    map.on('load', () => {
      map.addSource('full-route', { type: 'geojson', data: lineGeoJSON(routeCoords) });
      map.addLayer({
        id: 'full-route', type: 'line', source: 'full-route',
        layout: { 'line-join': 'round', 'line-cap': 'round' },
        paint: { 'line-color': '#d1d5db', 'line-width': 5 },
      });
      map.addSource('tracked', { type: 'geojson', data: lineGeoJSON([]) });
      map.addLayer({
        id: 'tracked', type: 'line', source: 'tracked',
        layout: { 'line-join': 'round', 'line-cap': 'round' },
        paint: { 'line-color': '#2563eb', 'line-width': 6 },
      });
    });

    function updatePosition(lat, lng) {
      const coord = [lng, lat];
      trackedCoords.push(coord);
      if (map.getSource('tracked')) {
        map.getSource('tracked').setData(lineGeoJSON(trackedCoords));
      }
      if (!posMarker) {
        const el = document.createElement('div');
        el.style.cssText = 'width:16px;height:16px;border-radius:50%;background:#2563eb;border:3px solid white;box-shadow:0 2px 8px rgba(0,0,0,0.3)';
        posMarker = new mapboxgl.Marker({ element: el }).setLngLat(coord).addTo(map);
      } else {
        posMarker.setLngLat(coord);
      }
      map.easeTo({ center: coord, duration: 800 });
    }

    window.addEventListener('message', (e) => {
      try {
        const data = JSON.parse(e.data);
        if (data.type === 'UPDATE_POSITION') updatePosition(data.latitude, data.longitude);
      } catch (_) {}
    });
  </script>
</body>
</html>`
}
