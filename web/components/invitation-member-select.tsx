import type { InvitationMember } from "@/lib/invitations";
import { MemberCombobox } from "@/components/member-combobox";

export function InvitationMemberSelect({
  id,
  members,
  value,
  onChange,
  invalid,
}: {
  id: string;
  members: InvitationMember[];
  value: string;
  onChange: (value: string) => void;
  invalid: boolean;
}) {
  return (
    <MemberCombobox
      id={id}
      members={members.map((member) => ({
        id: member.id,
        name: member.name,
        disabled: member.isLinked,
        hint: member.isLinked ? "Ya entró con su cuenta" : undefined,
      }))}
      value={value}
      onChange={onChange}
      invalid={invalid}
      placeholder="Buscá tu nombre"
    />
  );
}
