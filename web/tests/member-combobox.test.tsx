import { describe, it, expect, vi, afterEach } from "vitest";
import { useState } from "react";
import { render, screen, cleanup } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { MemberCombobox, type MemberOption } from "@/components/member-combobox";

// SPT-124: con muchos integrantes, el selector permite buscar escribiendo el nombre.
const members: MemberOption[] = Array.from({ length: 43 }, (_, index) => ({
  id: index + 1,
  name: `Integrante ${index + 1}`,
}));
members[20] = { id: 21, name: "Valentina" };
members[30] = { id: 31, name: "Bruno", disabled: true, hint: "Ya entró con su cuenta" };

function Harness({ onChange = vi.fn() }: { onChange?: (value: string) => void }) {
  const [value, setValue] = useState("");
  return (
    <>
      <label htmlFor="member">Pago</label>
      <MemberCombobox
        id="member"
        members={members}
        value={value}
        onChange={(next) => {
          setValue(next);
          onChange(next);
        }}
      />
    </>
  );
}

describe("MemberCombobox", () => {
  afterEach(() => {
    cleanup();
  });

  it("filtra los integrantes por lo que se escribe y elige uno", async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(<Harness onChange={onChange} />);

    await user.type(screen.getByLabelText("Pago"), "valen");

    expect(screen.getAllByRole("option").map((option) => option.textContent)).toEqual([
      "Valentina",
    ]);
    await user.click(screen.getByRole("option", { name: "Valentina" }));

    expect(onChange).toHaveBeenLastCalledWith("21");
    expect(screen.getByLabelText("Pago")).toHaveValue("Valentina");
  });

  it("avisa cuando ningún integrante coincide", async () => {
    const user = userEvent.setup();
    render(<Harness />);

    await user.type(screen.getByLabelText("Pago"), "zzz");

    expect(screen.queryAllByRole("option")).toHaveLength(0);
    expect(screen.getByText("No hay integrantes con ese nombre")).toBeInTheDocument();
  });

  it("muestra los integrantes deshabilitados sin dejar elegirlos", async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(<Harness onChange={onChange} />);

    await user.type(screen.getByLabelText("Pago"), "bru");
    const bruno = screen.getByRole("option", { name: /Bruno/ });
    expect(bruno).toHaveAttribute("aria-disabled", "true");
    expect(bruno).toHaveTextContent("Ya entró con su cuenta");
    await user.click(bruno);

    expect(onChange).not.toHaveBeenCalled();
  });
});
