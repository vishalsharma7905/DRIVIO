import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png';
import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';

// Fix Leaflet default marker icon paths for Vite production & dev builds
try {
  const leafletLib: any = (L as any)?.Icon ? L : (L as any)?.default;
  if (leafletLib?.Icon?.Default?.prototype) {
    delete leafletLib.Icon.Default.prototype._getIconUrl;
    leafletLib.Icon.Default.mergeOptions({
      iconRetinaUrl: markerIcon2x,
      iconUrl: markerIcon,
      shadowUrl: markerShadow,
    });
  }
} catch (err) {
  console.warn('Leaflet icon patch skipped:', err);
}

export default ((L as any)?.Icon ? L : (L as any)?.default) as typeof L;
