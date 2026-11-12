"use client";

import { Combobox as ComboboxPrimitive } from "@base-ui/react/combobox";
import { ChevronDown } from "lucide-react";

import { cn } from "@/lib/utils";

const Combobox = ComboboxPrimitive.Root;

function ComboboxInputGroup({ className, ...props }: ComboboxPrimitive.InputGroup.Props) {
  return (
    <ComboboxPrimitive.InputGroup
      data-slot="combobox-input-group"
      className={cn("relative", className)}
      {...props}
    />
  );
}

function ComboboxInput({ className, ...props }: ComboboxPrimitive.Input.Props) {
  return (
    <ComboboxPrimitive.Input
      data-slot="combobox-input"
      className={cn(
        "border-input text-text-primary placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-ring/50 aria-invalid:border-destructive h-11 w-full rounded-[6px] border bg-transparent py-2 pr-9 pl-3 text-base outline-none focus-visible:ring-3 disabled:cursor-not-allowed disabled:opacity-50",
        className
      )}
      {...props}
    />
  );
}

function ComboboxTrigger({ className, ...props }: ComboboxPrimitive.Trigger.Props) {
  return (
    <ComboboxPrimitive.Trigger
      data-slot="combobox-trigger"
      className={cn(
        "text-muted-foreground absolute inset-y-0 right-0 flex w-9 items-center justify-center",
        className
      )}
      {...props}
    >
      <ChevronDown aria-hidden="true" className="size-4" />
    </ComboboxPrimitive.Trigger>
  );
}

function ComboboxContent({
  className,
  sideOffset = 6,
  ...props
}: ComboboxPrimitive.Popup.Props & Pick<ComboboxPrimitive.Positioner.Props, "sideOffset">) {
  return (
    <ComboboxPrimitive.Portal>
      <ComboboxPrimitive.Positioner className="isolate z-50 outline-none" sideOffset={sideOffset}>
        <ComboboxPrimitive.Popup
          data-slot="combobox-content"
          className={cn(
            "bg-popover text-popover-foreground ring-foreground/10 w-(--anchor-width) max-w-(--available-width) rounded-[16px] p-1 shadow-md ring-1 outline-none",
            className
          )}
          {...props}
        />
      </ComboboxPrimitive.Positioner>
    </ComboboxPrimitive.Portal>
  );
}

function ComboboxEmpty({ className, children, ...props }: ComboboxPrimitive.Empty.Props) {
  return (
    <ComboboxPrimitive.Empty data-slot="combobox-empty" {...props}>
      <p className={cn("text-text-secondary px-3 py-2 text-sm font-medium", className)}>
        {children}
      </p>
    </ComboboxPrimitive.Empty>
  );
}

function ComboboxList({ className, ...props }: ComboboxPrimitive.List.Props) {
  return (
    <ComboboxPrimitive.List
      data-slot="combobox-list"
      className={cn(
        "max-h-[min(16rem,var(--available-height))] overflow-y-auto overscroll-contain",
        className
      )}
      {...props}
    />
  );
}

function ComboboxItem({ className, ...props }: ComboboxPrimitive.Item.Props) {
  return (
    <ComboboxPrimitive.Item
      data-slot="combobox-item"
      className={cn(
        "text-text-primary data-highlighted:bg-muted flex cursor-default flex-col rounded-[10px] px-3 py-2 text-sm font-medium outline-none select-none data-disabled:opacity-50",
        className
      )}
      {...props}
    />
  );
}

export {
  Combobox,
  ComboboxInputGroup,
  ComboboxInput,
  ComboboxTrigger,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxList,
  ComboboxItem,
};
