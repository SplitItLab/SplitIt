"use client";

import {
  Combobox,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxInput,
  ComboboxInputGroup,
  ComboboxItem,
  ComboboxList,
  ComboboxTrigger,
} from "@/components/ui/combobox";

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
    <Combobox<MemberOption>
      items={members}
      value={selected}
      onValueChange={(member) => onChange(member ? String(member.id) : "")}
      itemToStringLabel={(member) => member.name}
      isItemEqualToValue={(item, current) => item.id === current.id}
      disabled={disabled}
    >
      <ComboboxInputGroup className={className}>
        <ComboboxInput id={id} placeholder={placeholder} aria-invalid={invalid} />
        <ComboboxTrigger aria-label="Ver integrantes" />
      </ComboboxInputGroup>

      <ComboboxContent>
        <ComboboxEmpty>No hay integrantes con ese nombre</ComboboxEmpty>
        <ComboboxList>
          {(member: MemberOption) => (
            <ComboboxItem key={member.id} value={member} disabled={member.disabled}>
              <span className="truncate">{member.name}</span>
              {member.hint && (
                <span className="text-text-secondary text-xs font-medium">{member.hint}</span>
              )}
            </ComboboxItem>
          )}
        </ComboboxList>
      </ComboboxContent>
    </Combobox>
  );
}
