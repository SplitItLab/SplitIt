"use client";
import Image from "next/image";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Field, FieldLabel, FieldError, FieldGroup } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Spinner } from "@/components/ui/spinner";
import { zodResolver } from "@hookform/resolvers/zod";
import { registerUser, RegisterError, RegisterInput, registerSchema } from "@/lib/auth";

export default function RegisterPage() {
  const router = useRouter();
  const [generalError, setGeneralError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterInput>({
    resolver: zodResolver(registerSchema),
  });

  const onSubmit = async (data: RegisterInput) => {
    setGeneralError(null);
    try {
      await registerUser(data);
      router.push("/login");
    } catch (err) {
      if (err instanceof RegisterError) {
        if (err.type === "email-taken") {
          setError("email", { message: err.message });
        } else {
          setGeneralError(err.message);
        }
      }
    }
  };

  return (
    <div className="relative grid h-screen md:grid-cols-2">
      <Link href="/login" className="absolute top-6 right-6 text-sm font-medium">
        Login
      </Link>

      <div className="relative hidden h-screen overflow-hidden md:block">
        <Image src="/register-image.png" alt="" fill className="object-cover" />
      </div>

      <div className="flex items-center justify-center overflow-y-auto p-6">
        <div className="w-full max-w-sm">
          <h1 className="text-2xl font-bold">Crea una cuenta</h1>
          <p className="text-muted-foreground mb-6 text-sm">
            Ingresá mail y contraseña para crear tu cuenta
          </p>

          <form onSubmit={handleSubmit(onSubmit)} noValidate>
            <FieldGroup>
              {generalError && (
                <Alert variant="destructive">
                  <AlertDescription>{generalError}</AlertDescription>
                </Alert>
              )}

              <Field data-invalid={!!errors.name}>
                <FieldLabel htmlFor="name">Nombre</FieldLabel>
                <Input
                  id="name"
                  placeholder="Nombre"
                  autoComplete="name"
                  aria-invalid={!!errors.name}
                  {...register("name")}
                />
                <FieldError errors={[errors.name]} />
              </Field>

              <Field data-invalid={!!errors.email}>
                <FieldLabel htmlFor="email">Email</FieldLabel>
                <Input
                  id="email"
                  placeholder="Email"
                  autoComplete="email"
                  type="email"
                  aria-invalid={!!errors.email}
                  {...register("email")}
                />
                <FieldError errors={[errors.email]} />
              </Field>

              <Field data-invalid={!!errors.password}>
                <FieldLabel htmlFor="password">Contraseña</FieldLabel>
                <Input
                  id="password"
                  placeholder="Contraseña"
                  type="password"
                  autoComplete="new-password"
                  aria-invalid={!!errors.password}
                  {...register("password")}
                />
                <FieldError errors={[errors.password]} />
              </Field>

              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? <Spinner /> : "Crear cuenta"}
              </Button>
            </FieldGroup>
          </form>
        </div>
      </div>
    </div>
  );
}
