interface StatusBadgeProps {
  isAuthorized: boolean;
}

export default function StatusBadge({ isAuthorized }: StatusBadgeProps) {
  return (
    <div className="flex items-center gap-2 rounded-full bg-white/10 px-3 py-1 text-xs font-medium">
      <span
        className={`h-2.5 w-2.5 rounded-full ${isAuthorized ? 'bg-yellow-300' : 'bg-gray-300'}`}
        aria-hidden="true"
      />
      <span>{isAuthorized ? 'Authorized' : 'Unauthorized'}</span>
    </div>
  );
}
