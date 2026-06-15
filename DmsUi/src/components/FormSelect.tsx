interface SelectProps {
  label: string;
  value: string;
  onChange: (v: string) => void;
  options: any[];
}

export const FormSelect = ({
  label,
  value,
  onChange,
  options,
}: SelectProps) => (
  <div className="flex items-center gap-3">
    <label className="w-32 text-sm font-medium">{label}</label>
    <select
      className="flex-1 border rounded px-3 py-2"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      required
    >
      <option value="">Select</option>
      {options.map((o: any, i: number) => (
        <option key={i} value={o[0]}>
          {o[1] || o[0]}
        </option>
      ))}
    </select>
  </div>
);

export const FormSelectSimple = ({
  label,
  value,
  onChange,
  options,
}: any) => {
  if (!options || options.length === 0) return null;

  return (
    <div className="flex items-center gap-3">
      <label className="w-32 text-sm font-medium">{label}</label>
      <select
        className="flex-1 border rounded px-3 py-2"
        value={value}
        onChange={(e) => onChange(e.target.value)}
      >
        <option value="">Select</option>
        {options.map((o: string) => (
          <option key={o} value={o}>
            {o}
          </option>
        ))}
      </select>
    </div>
  );
};
