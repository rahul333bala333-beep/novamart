"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Search, UserCheck, UserX, Users } from "lucide-react";
import * as React from "react";
import { AdminPageHeader } from "@/components/admin/page-header";
import { DataTable, type Column } from "@/components/admin/data-table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ConfirmDialog, Dialog } from "@/components/ui/dialog";
import { EmptyState, ErrorState } from "@/components/ui/empty-state";
import { Pagination } from "@/components/ui/pagination";
import { useToast } from "@/components/ui/toast";
import { authApi } from "@/lib/api/resources";
import { formatDate, initialsOf } from "@/lib/format";
import type { Role, UserProfile } from "@/lib/types";

export default function AdminUsersPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [page, setPage] = React.useState(0);
  const [term, setTerm] = React.useState("");
  const [search, setSearch] = React.useState("");

  // Selected user for status/role dialog
  const [statusUser, setStatusUser] = React.useState<UserProfile | null>(null);
  const [roleUser, setRoleUser] = React.useState<UserProfile | null>(null);
  const [selectedRole, setSelectedRole] = React.useState<Role>("USER");

  // Debounced so typing a name does not fire a request per keystroke.
  React.useEffect(() => {
    const timer = window.setTimeout(() => {
      setSearch(term.trim());
      setPage(0);
    }, 300);
    return () => window.clearTimeout(timer);
  }, [term]);

  const users = useQuery({
    queryKey: ["users", { page, search }],
    queryFn: () => authApi.listUsers({ page, size: 20, search: search || undefined }),
    placeholderData: (previous) => previous,
  });

  const updateStatus = useMutation({
    mutationFn: ({ id, enabled }: { id: string; enabled: boolean }) =>
      authApi.updateUserStatus(id, enabled),
    onSuccess: (updated) => {
      void queryClient.invalidateQueries({ queryKey: ["users"] });
      toast.success(`Account for ${updated.firstName} ${updated.lastName} has been ${updated.enabled ? "enabled" : "disabled"}`);
      setStatusUser(null);
    },
    onError: (err: Error) => {
      toast.error(err.message || "Failed to update user status");
    },
  });

  const updateRole = useMutation({
    mutationFn: ({ id, roles }: { id: string; roles: Role[] }) =>
      authApi.updateUserRole(id, roles),
    onSuccess: (updated) => {
      void queryClient.invalidateQueries({ queryKey: ["users"] });
      toast.success(`Roles updated for ${updated.firstName} ${updated.lastName}`);
      setRoleUser(null);
    },
    onError: (err: Error) => {
      toast.error(err.message || "Failed to update user role");
    },
  });

  const columns: Column<UserProfile>[] = [
    {
      key: "name",
      header: "Customer",
      cell: (user) => (
        <div className="flex items-center gap-3">
          <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-sunken text-[length:--text-caption] font-semibold text-ink-soft">
            {initialsOf(user.firstName, user.lastName)}
          </span>
          <div className="min-w-0">
            <p className="truncate font-medium text-ink">
              {user.firstName} {user.lastName}
            </p>
            <p className="truncate text-[length:--text-caption] text-muted">{user.email}</p>
          </div>
        </div>
      ),
    },
    {
      key: "phone",
      header: "Phone",
      showOnMobile: false,
      cell: (user) => <span className="tabular text-muted">{user.phone ?? "-"}</span>,
    },
    {
      key: "roles",
      header: "Role",
      cell: (user) => (
        <div className="flex gap-1">
          {user.roles.map((role) => (
            <Badge key={role} tone={role === "ADMIN" ? "solid" : "neutral"}>
              {role}
            </Badge>
          ))}
        </div>
      ),
    },
    {
      key: "status",
      header: "Status",
      cell: (user) =>
        user.enabled ? <Badge tone="success">Active</Badge> : <Badge tone="danger">Disabled</Badge>,
    },
    {
      key: "joined",
      header: "Joined",
      showOnMobile: false,
      cell: (user) => <span className="text-muted text-xs">{formatDate(user.createdAt)}</span>,
    },
    {
      key: "actions",
      header: "Actions",
      align: "right",
      cell: (user) => (
        <div className="flex items-center justify-end gap-2">
          <Button
            size="sm"
            variant="ghost"
            onClick={() => {
              setRoleUser(user);
              setSelectedRole(user.roles.includes("ADMIN") ? "ADMIN" : "USER");
            }}
          >
            Edit Role
          </Button>

          <Button
            size="sm"
            variant="ghost"
            className={user.enabled ? "text-danger hover:bg-danger-soft" : "text-success hover:bg-emerald-50"}
            onClick={() => setStatusUser(user)}
          >
            {user.enabled ? <UserX className="size-3.5 mr-1" /> : <UserCheck className="size-3.5 mr-1" />}
            {user.enabled ? "Disable" : "Enable"}
          </Button>
        </div>
      ),
    },
  ];

  if (users.isError) {
    return <ErrorState description="We could not load customers." onRetry={() => users.refetch()} />;
  }

  return (
    <div>
      <AdminPageHeader
        title="Customers & Users"
        description={users.data ? `${users.data.page.totalElements} registered accounts` : undefined}
      />

      <div className="relative mb-4 max-w-sm">
        <Search
          className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted"
          aria-hidden="true"
        />
        <input
          type="search"
          value={term}
          onChange={(event) => setTerm(event.target.value)}
          placeholder="Search by name or email"
          aria-label="Search customers"
          className="h-10 w-full rounded-[--radius-md] border border-line-strong bg-surface pl-9 pr-3 text-[length:--text-body] text-ink placeholder:text-muted"
        />
      </div>

      <DataTable
        caption="Registered customers"
        columns={columns}
        rows={users.data?.content ?? []}
        keyOf={(user) => user.id}
        loading={users.isLoading}
        emptyState={
          <EmptyState
            icon={Users}
            title={search ? "No customers match that search" : "No customers yet"}
            description={search ? "Try a different name or email." : undefined}
          />
        }
      />

      {users.data && <Pagination meta={users.data.page} onPageChange={setPage} className="mt-6" />}

      {/* Confirm Status Toggle Dialog */}
      {statusUser && (
        <ConfirmDialog
          open={!!statusUser}
          onClose={() => setStatusUser(null)}
          onConfirm={() =>
            updateStatus.mutate({
              id: statusUser.id,
              enabled: !statusUser.enabled,
            })
          }
          title={statusUser.enabled ? `Disable ${statusUser.firstName}'s account?` : `Enable ${statusUser.firstName}'s account?`}
          description={
            statusUser.enabled
              ? "Disabled users cannot sign in or place new orders on Nova Mart."
              : "This user will regain full customer access to sign in and place orders."
          }
          confirmLabel={statusUser.enabled ? "Disable Account" : "Enable Account"}
          loading={updateStatus.isPending}
          destructive={statusUser.enabled}
        />
      )}

      {/* Role Management Modal */}
      {roleUser && (
        <Dialog
          open={Boolean(roleUser)}
          onClose={() => setRoleUser(null)}
          title="Manage Roles & Permissions"
          description={`Select account role for ${roleUser.firstName} ${roleUser.lastName} (${roleUser.email})`}
          footer={
            <>
              <Button variant="secondary" onClick={() => setRoleUser(null)}>
                Cancel
              </Button>
              <Button
                loading={updateRole.isPending}
                onClick={() =>
                  updateRole.mutate({
                    id: roleUser.id,
                    roles: selectedRole === "ADMIN" ? ["USER", "ADMIN"] : ["USER"],
                  })
                }
              >
                Save Role
              </Button>
            </>
          }
        >
          <div className="space-y-3 py-2">
            <label
              className={`flex cursor-pointer items-start gap-3 rounded-[--radius-lg] border p-4 transition-colors ${
                selectedRole === "USER" ? "border-ink bg-sunken" : "border-line bg-surface"
              }`}
            >
              <input
                type="radio"
                name="role"
                value="USER"
                checked={selectedRole === "USER"}
                onChange={() => setSelectedRole("USER")}
                className="mt-1"
              />
              <div>
                <p className="font-semibold text-ink">USER (Customer Access)</p>
                <p className="text-xs text-muted mt-0.5">
                  Standard shopper account. Full access to browse catalogue, add to cart/wishlist, checkout, submit reviews, and track orders.
                </p>
              </div>
            </label>

            <label
              className={`flex cursor-pointer items-start gap-3 rounded-[--radius-lg] border p-4 transition-colors ${
                selectedRole === "ADMIN" ? "border-ink bg-sunken" : "border-line bg-surface"
              }`}
            >
              <input
                type="radio"
                name="role"
                value="ADMIN"
                checked={selectedRole === "ADMIN"}
                onChange={() => setSelectedRole("ADMIN")}
                className="mt-1"
              />
              <div>
                <p className="font-semibold text-ink">ADMIN (Full Back-Office Access)</p>
                <p className="text-xs text-muted mt-0.5">
                  Privileged administrator access. Manages products, categories, stock, orders, coupons, customer accounts, and analytics.
                </p>
              </div>
            </label>
          </div>
        </Dialog>
      )}
    </div>
  );
}
