export function SplitItLogo() {
  return (
    <div className="relative h-[34px] w-[74px]">
      <div className="bg-primary absolute top-0 left-0 h-[34px] w-[57px] rounded-[8px]" />
      <div className="absolute top-[3px] left-[calc(50%+1px)] flex -translate-x-1/2 items-center justify-center text-[24px] leading-[1.15] font-extrabold whitespace-nowrap">
        <span className="text-primary-foreground">Split</span>
        <span className="text-black">It</span>
      </div>
    </div>
  );
}
