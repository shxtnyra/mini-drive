interface MenuGroupProps {
  children: React.ReactNode;
  showBorder?: boolean;
}

export default function MenuGroup({ children, showBorder = true }: MenuGroupProps) {
  return (
    <div className={`py-1 ${showBorder ? 'border-b border-border last:border-b-0' : ''}`}>
      {children}
    </div>
  );
}