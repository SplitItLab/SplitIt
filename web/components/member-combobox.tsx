"use client";

import { Combobox } from "@base-ui/react/combobox";
import { ChevronDown } from "lucide-react";

import { cn } from "@/lib/utils";

export type MemberOption = {
  id: number;
  name: string;
  disabled?: boolean;
  /** Texto secundario que acompaña al nombre en la lista. */
  hint?: string;
};

/**
 * Selector de integrante que permite buscar escribiendo el nombre: con muchos integrantes un
 * desplegable común obliga a recorrer toda la lista.
 */
export function MemberCombobox({
  id,
  members,
  value,
  onChange,
  invalid = false,
  disabled = false,
  placeholder,
  className,
}: {
  id: string;
  members: MemberOption[];
  /** Id del integrante elegido, o "" si no hay ninguno. */
  value: string;
  onChange: (value: string) => void;
  invalid?: boolean;
  disabled?: boolean;
  placeholder?: string;
  className?: string;
}) {
  const selected = members.find((member) => String(member.id) === value) ?? null;

  return (
    <Combobox.Root<MemberOption>
      items={members}
      value={selected}
      onValueChange={(member) => onChange(member ? String(member.id) : "")}
      itemToStringLabel={(member) => member.name}
      isItemEqualToValue={(item, current) => item.id === current.id}
      disabled={disabled}
    >
      <Combobox.InputGroup className={cn("relative", className)}>
        <Combobox.Input
          id={id}
          placeholder={placeholder}
          aria-invalid={invalid}
          className="border-input text-text-primary placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-ring/50 aria-invalid:border-destructive h-11 w-full rounded-[6px] border bg-transparent py-2 pr-9 pl-3 text-base outline-none focus-visible:ring-3 disabled:cursor-not-allowed disabled:opacity-50"
        />
        <Combobox.Trigger
          aria-label="Ver integrantes"
          className="text-muted-foreground absolute inset-y-0 right-0 flex w-9 items-center justify-center"
        >
          <ChevronDown aria-hidden="true" className="size-4" />
        </Combobox.Trigger>
      </Combobox.InputGroup>

      <Combobox.Portal>
        <Combobox.Positioner className="isolate z-50 outline-none" sideOffset={6}>
          <Combobox.Popup className="bg-popover text-popover-foreground ring-foreground/10 w-(--anchor-width) max-w-(--available-width) rounded-[16px] p-1 shadow-md ring-1 outline-none">
            <Combobox.Empty>
              <p className="text-text-secondary px-3 py-2 text-sm font-medium">
                No hay integrantes con ese nombre
              </p>
            </Combobox.Empty>
            <Combobox.List className="max-h-[min(16rem,var(--available-height))] overflow-y-auto overscroll-contain">
              {(member: MemberOption) => (
                <Combobox.Item
                  key={member.id}
                  value={member}
                  disabled={member.disabled}
                  className="text-text-primary data-highlighted:bg-muted flex cursor-default flex-col rounded-[10px] px-3 py-2 text-sm font-medium outline-none select-none data-disabled:opacity-50"
                >
                  <span className="truncate">{member.name}</span>
                  {member.hint && (
                    <span className="text-text-secondary text-xs font-medium">{member.hint}</span>
                  )}
                </Combobox.Item>
              )}
            </Combobox.List>
          </Combobox.Popup>
        </Combobox.Positioner>
      </Combobox.Portal>
    </Combobox.Root>
  );
}
