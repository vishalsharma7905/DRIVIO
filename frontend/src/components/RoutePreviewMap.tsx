import React, { useEffect, useMemo, useState } from 'react';
import { MapContainer, TileLayer, Marker, Polyline, Popup, useMap, useMapEvents } from 'react-leaflet';
import L from '../utils/leafletFix';
import { useTheme } from '../context/ThemeContext';

interface RoutePreviewMapProps {
  pickupLat: number;
  pickupLng: number;
  pickupAddress: string;
  destLat: number;
  destLng: number;
  destAddress: string;
  routeGeometry: [number, number][];
  onMapPick?: (target: 'PICKUP' | 'DEST', lat: number, lng: number) => void;
}

const FitBoundsHelper: React.FC<{ points: [number, number][] }> = ({ points }) => {
  const map = useMap();
  useEffect(() => {
    if (points.length >= 2) {
      map.fitBounds(L.latLngBounds(points), { padding: [36, 36] });
    }
  }, [map, points]);
  return null;
};

const MapClickHandler: React.FC<{
  pickTarget: 'PICKUP' | 'DEST';
  onPick?: (target: 'PICKUP' | 'DEST', lat: number, lng: number) => void;
}> = ({ pickTarget, onPick }) => {
  useMapEvents({
    click(e) {
      if (onPick) onPick(pickTarget, e.latlng.lat, e.latlng.lng);
    },
  });
  return null;
};

export const RoutePreviewMap: React.FC<RoutePreviewMapProps> = ({
  pickupLat,
  pickupLng,
  pickupAddress,
  destLat,
  destLng,
  destAddress,
  routeGeometry,
  onMapPick,
}) => {
  useTheme();
  const [pickTarget, setPickTarget] = useState<'PICKUP' | 'DEST'>('DEST');

  const pickupPin = useMemo(
    () =>
      L.divIcon({
        className: 'drivio-pickup-pin',
        html: `<div style="background:#22C55E;width:14px;height:14px;border-radius:50%;border:3px solid #fff;box-shadow:0 0 10px rgba(34,197,94,0.8)"></div>`,
        iconSize: [14, 14],
        iconAnchor: [7, 7],
      }),
    []
  );

  const destPin = useMemo(
    () =>
      L.divIcon({
        className: 'drivio-dest-pin',
        html: `<div style="background:#F59E0B;width:14px;height:14px;border-radius:50%;border:3px solid #fff;box-shadow:0 0 10px rgba(245,158,11,0.8)"></div>`,
        iconSize: [14, 14],
        iconAnchor: [7, 7],
      }),
    []
  );

  const tileUrl =
    import.meta.env.VITE_MAP_TILE_URL || 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';

  const polylinePositions: [number, number][] =
    routeGeometry.length >= 2
      ? routeGeometry
      : [
          [pickupLat, pickupLng],
          [destLat, destLng],
        ];

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between text-[11px]">
        <span className="text-slate-400">Click map to pin:</span>
        <div className="inline-flex rounded-lg bg-[#141615] border border-[#2F3431] p-0.5">
          <button
            type="button"
            onClick={() => setPickTarget('PICKUP')}
            className={`px-2.5 py-1 rounded-md font-semibold transition ${
              pickTarget === 'PICKUP' ? 'bg-emerald-500 text-[#09130C]' : 'text-slate-400 hover:text-white'
            }`}
          >
            Pickup
          </button>
          <button
            type="button"
            onClick={() => setPickTarget('DEST')}
            className={`px-2.5 py-1 rounded-md font-semibold transition ${
              pickTarget === 'DEST' ? 'bg-amber-500 text-[#09130C]' : 'text-slate-400 hover:text-white'
            }`}
          >
            Destination
          </button>
        </div>
      </div>

      <div className="h-56 w-full rounded-xl overflow-hidden border border-[#2F3431] z-0 relative">
        <MapContainer
          center={[pickupLat, pickupLng]}
          zoom={12}
          style={{ height: '100%', width: '100%' }}
          scrollWheelZoom={false}
        >
          <TileLayer url={tileUrl} attribution="&copy; OpenStreetMap contributors" />
          <Marker position={[pickupLat, pickupLng]} icon={pickupPin}>
            <Popup>Pickup: {pickupAddress}</Popup>
          </Marker>
          <Marker position={[destLat, destLng]} icon={destPin}>
            <Popup>Destination: {destAddress}</Popup>
          </Marker>
          <Polyline positions={polylinePositions} pathOptions={{ color: '#22C55E', weight: 4, opacity: 0.9 }} />
          <FitBoundsHelper points={polylinePositions} />
          <MapClickHandler pickTarget={pickTarget} onPick={onMapPick} />
        </MapContainer>
      </div>
    </div>
  );
};
