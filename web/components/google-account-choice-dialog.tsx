"use client";

import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import type { ExistingAccountChoice } from "@/lib/auth";

type GoogleAccountChoiceDialogProps = {
  open: boolean;
  onChoose: (choice: ExistingAccountChoice) => void;
  onCancel: () => void;
};

export function GoogleAccountChoiceDialog({
  open,
  onChoose,
  onCancel,
}: GoogleAccountChoiceDialogProps) {
  return (
    <Dialog open={open} onOpenChange={(next) => !next && onCancel()}>
      <DialogContent
        aria-label="Ya tenés una cuenta"
        className="border-border shadow-none sm:max-w-[480px]"
      >
        <div>
          <DialogTitle className="text-[28px]">Ya tenés una cuenta con este email</DialogTitle>
          <DialogDescription className="text-muted-foreground text-sm leading-6 font-medium">
            Elegí qué hacemos con ella. En los dos casos, desde ahora vas a ingresar con Google y tu
            contraseña deja de servir.
          </DialogDescription>
        </div>

        <ul className="flex flex-col gap-3 text-sm leading-6">
          <li>
            <strong>Vincular:</strong> seguís con la misma cuenta, con tus eventos y gastos.
          </li>
          <li>
            <strong>Reemplazar:</strong> se crea una cuenta nueva con los datos de Google, se mueven
            a ella todos tus eventos y la cuenta anterior queda deshabilitada.
          </li>
        </ul>

        <div className="flex flex-col-reverse gap-3 pt-2 sm:flex-row sm:justify-end">
          <Button
            type="button"
            variant="ghost"
            onClick={onCancel}
            className="text-muted-foreground hover:text-foreground mt-0 h-10 rounded-[8px] border-0 px-4 text-xl font-medium"
          >
            Cancelar
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={() => onChoose("REPLACE")}
            className="h-10 rounded-[8px] px-4 text-xl font-medium"
          >
            Reemplazar
          </Button>
          <Button
            type="button"
            onClick={() => onChoose("LINK")}
            className="h-10 rounded-[8px] px-4 text-xl font-medium"
          >
            Vincular
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
