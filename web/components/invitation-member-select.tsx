import type { InvitationMember } from "@/lib/invitations";

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
    <select
      id={id}
      value={value}
      onChange={(event) => onChange(event.target.value)}
      aria-invalid={invalid}
      className="border-border text-foreground focus-visible:border-ring focus-visible:ring-ring/50 aria-invalid:border-destructive h-auto w-full rounded-[6px] border bg-white px-3 py-2 text-base leading-6 outline-none focus-visible:ring-3"
    >
      <option value="" disabled>
        Elegite de la lista
      </option>
      {members.map((member) => (
        <option key={member.id} value={member.id} disabled={member.isLinked}>
          {member.isLinked ? `${member.name} · ya entró con su cuenta` : member.name}
        </option>
      ))}
    </select>
  );
}
