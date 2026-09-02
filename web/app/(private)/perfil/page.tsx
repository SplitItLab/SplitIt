"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { LogOut } from "lucide-react";
import {
  getProfile,
  updateProfile,
  getInitials,
  ProfileError,
  Profile,
  ProfileInput,
  profileSchema,
} from "@/lib/profile";
import { Card, CardContent } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Field, FieldLabel, FieldError, FieldGroup } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Spinner } from "@/components/ui/spinner";

export default function ProfilePage() {
  const router = useRouter();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [generalError, setGeneralError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ProfileInput>({
    resolver: zodResolver(profileSchema),
  });

  useEffect(() => {
    let cancelled = false;

    getProfile()
      .then((data) => {
        if (cancelled) return;
        setProfile(data);
      })
      .catch((err) => {
        if (cancelled) return;
        if (err instanceof ProfileError && err.type === "unauthorized") {
          router.replace("/login");
          return;
        }
        setLoadError(err instanceof ProfileError ? err.message : "Ocurrió un error inesperado.");
      });

    return () => {
      cancelled = true;
    };
  }, [router]);

  const startEditing = () => {
    if (!profile) return;
    reset({ name: profile.name, email: profile.email });
    setGeneralError(null);
    setEditing(true);
  };

  const onSubmit = async (data: ProfileInput) => {
    setGeneralError(null);
    try {
      const updated = await updateProfile(data);
      setProfile(updated);
      setEditing(false);
    } catch (err) {
      if (err instanceof ProfileError) {
        if (err.type === "unauthorized") {
          router.replace("/login");
          return;
        }
        if (err.field) {
          setError(err.field, { message: err.message });
          return;
        }
        setGeneralError(err.message);
        return;
      }
      throw err;
    }
  };

  if (loadError) {
    return (
      <div className="mx-auto w-full max-w-md p-4 sm:p-6">
        <Alert variant="destructive">
          <AlertDescription>{loadError}</AlertDescription>
        </Alert>
        <Button className="mt-4" variant="outline" onClick={() => window.location.reload()}>
          Reintentar
        </Button>
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Spinner />
      </div>
    );
  }

  return (
    <div className="relative flex flex-col px-4 pt-6 pb-10 sm:px-[40px] sm:pt-10 sm:pb-[104px]">
      <h1 className="text-text-primary text-3xl leading-[115%] font-extrabold sm:text-[60px]">
        Tu perfil
      </h1>
      <p className="text-text-secondary mt-4 text-[14px] font-medium sm:mt-6">
        Configura las preferencias de tu perfil
      </p>
      <Card className="mt-2 sm:mt-4">
        <CardContent>
          <div className="flex flex-col gap-6 px-4 py-4 sm:px-[32px] sm:py-[32px]">
            <div className="flex items-center gap-3">
              <Avatar className="size-14">
                <AvatarFallback className="bg-primary/15 text-primary text-lg font-semibold">
                  {getInitials(profile.name)}
                </AvatarFallback>
              </Avatar>
              <div>
                <p className="text-text-primary text-[32px] font-extrabold">{profile.name}</p>
                <p className="text-muted-foreground text-sm">{profile.email}</p>
              </div>
            </div>
            <p className="text-muted-foreground text-sm">Actualiza tu información personal</p>

            {!editing ? (
              <Button className="h-11 gap-2.5 self-start rounded-md px-5" onClick={startEditing}>
                Editar
              </Button>
            ) : (
              <form onSubmit={handleSubmit(onSubmit)} noValidate>
                <FieldGroup>
                  {generalError && (
                    <Alert variant="destructive">
                      <AlertDescription>{generalError}</AlertDescription>
                    </Alert>
                  )}
                  <div className="grid gap-4 sm:grid-cols-2">
                    <Field className="gap-1.5" data-invalid={!!errors.name}>
                      <FieldLabel htmlFor="name" className="font-bold">
                        Nombre completo
                      </FieldLabel>
                      <Input
                        id="name"
                        autoComplete="name"
                        aria-invalid={!!errors.name}
                        className="h-11"
                        {...register("name")}
                      />
                      <FieldError errors={[errors.name]} />
                    </Field>

                    <Field className="gap-1.5" data-invalid={!!errors.email}>
                      <FieldLabel htmlFor="email" className="font-bold">
                        Email
                      </FieldLabel>
                      <Input
                        id="email"
                        type="email"
                        autoComplete="email"
                        aria-invalid={!!errors.email}
                        className="h-11"
                        {...register("email")}
                      />
                      <FieldError errors={[errors.email]} />
                    </Field>
                  </div>

                  <div className="flex gap-2">
                    <Button
                      type="submit"
                      disabled={isSubmitting}
                      className="h-11 gap-2.5 rounded-md px-5"
                    >
                      {isSubmitting ? <Spinner /> : "Guardar cambios"}
                    </Button>
                  </div>
                </FieldGroup>
              </form>
            )}
          </div>
        </CardContent>
      </Card>

      <Card className="mt-4 sm:mt-6">
        <CardContent>
          <div className="flex flex-col gap-6 px-4 py-4 sm:px-[32px] sm:py-[32px]">
            <div className="flex items-center gap-2">
              <LogOut className="size-5" />
              <p className="font-bold">Sesión</p>
            </div>
            <p className="text-muted-foreground text-sm">
              Tu cuenta y tus eventos se mantienen; podés volver a entrar cuando quieras
            </p>
            <div className="flex flex-col items-start justify-between gap-3 sm:flex-row sm:items-center">
              <div className="flex flex-col gap-6">
                <p className="font-bold">Cerrar sesión</p>
                <p className="text-muted-foreground text-sm">
                  Salir de tu cuenta en este dispositivo
                </p>
              </div>
              {/* Sin comportamiento todavía: la acción de cerrar sesión se implementa en SPT-50 */}
              <Button
                type="button"
                variant="ghost"
                className="border-border h-11 gap-2.5 rounded-md border bg-[#F1F5F9] px-4 text-black hover:bg-[#F1F5F9]"
              >
                Cerrar sesión
                <LogOut className="size-4" />
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
