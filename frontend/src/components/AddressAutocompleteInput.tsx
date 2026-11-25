import React, { useState, useEffect, useRef } from 'react';
import { MapPin, Loader2 } from 'lucide-react';
import api from '../services/api';

interface Suggestion {
  displayName: string;
  latitude: number;
  longitude: number;
  provider: string;
}

interface AddressAutocompleteInputProps {
  value: string;
  onChange: (address: string) => void;
  onSelectLocation: (address: string, lat: number, lng: number) => void;
  placeholder?: string;
  iconColorClass?: string;
  required?: boolean;
}

export const AddressAutocompleteInput: React.FC<AddressAutocompleteInputProps> = ({
  value,
  onChange,
  onSelectLocation,
  placeholder = 'Search location...',
  iconColorClass = 'text-emerald-400',
  required = true,
}) => {
  const [suggestions, setSuggestions] = useState<Suggestion[]>([]);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const skipNextFetch = useRef(false);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  useEffect(() => {
    if (skipNextFetch.current) {
      skipNextFetch.current = false;
      return;
    }
    if (!value || value.trim().length < 3) {
      setSuggestions([]);
      return;
    }
    const timer = setTimeout(async () => {
      try {
        setLoading(true);
        const res = await api.get<Suggestion[]>(`/routing/geocode?q=${encodeURIComponent(value.trim())}`);
        setSuggestions(res.data || []);
        setOpen((res.data || []).length > 0);
      } catch {
        setSuggestions([]);
      } finally {
        setLoading(false);
      }
    }, 350);
    return () => clearTimeout(timer);
  }, [value]);

  const handleSelect = (item: Suggestion) => {
    skipNextFetch.current = true;
    onSelectLocation(item.displayName, item.latitude, item.longitude);
    setOpen(false);
    setSuggestions([]);
  };

  return (
    <div ref={containerRef} className="relative">
      <MapPin className={`w-4 h-4 ${iconColorClass} absolute left-3 top-3`} />
      <input
        type="text"
        required={required}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onFocus={() => { if (suggestions.length > 0) setOpen(true); }}
        placeholder={placeholder}
        className="w-full bg-slate-950 border border-slate-700 rounded-xl pl-9 pr-9 py-2.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-emerald-500"
      />
      {loading && <Loader2 className="w-4 h-4 text-emerald-400 animate-spin absolute right-3 top-3" />}

      {open && suggestions.length > 0 && (
        <div className="absolute z-50 left-0 right-0 mt-1 bg-[#1C1F1D] border border-[#2F3431] rounded-xl shadow-2xl max-h-52 overflow-y-auto divide-y divide-[#2F3431]">
          {suggestions.map((s, idx) => (
            <button
              key={`${s.latitude}-${s.longitude}-${idx}`}
              type="button"
              onClick={() => handleSelect(s)}
              className="w-full text-left px-3 py-2.5 text-xs text-slate-200 hover:bg-emerald-500/10 flex items-start space-x-2 transition-colors"
            >
              <MapPin className="w-3.5 h-3.5 text-emerald-400 shrink-0 mt-0.5" />
              <span className="line-clamp-2">{s.displayName}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
};
