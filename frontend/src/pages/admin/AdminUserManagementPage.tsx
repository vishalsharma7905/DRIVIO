import React, { useState, useEffect } from 'react';
import { adminApi } from '../../services/api';
import { User, Role } from '../../types';
import { 
  Users, 
  Search, 
  Shield, 
  UserCheck, 
  UserX, 
  Car, 
  AlertCircle
} from 'lucide-react';

export const AdminUserManagementPage: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [roleFilter, setRoleFilter] = useState<Role | 'ALL'>('ALL');
  const [search, setSearch] = useState('');
  const [actionLoading, setActionLoading] = useState<number | null>(null);

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const res = await adminApi.getAllUsers();
      setUsers(res.data);
    } catch (err) {
      console.error('Failed to load users', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleToggleStatus = async (user: User) => {
    const action = user.isActive ? 'suspend' : 'activate';
    if (!window.confirm(`Are you sure you want to ${action} ${user.fullName}'s account?`)) return;

    try {
      setActionLoading(user.id);
      await adminApi.toggleUserStatus(user.id, !user.isActive);
      await fetchUsers();
    } catch (err: any) {
      alert(err.response?.data?.message || `Failed to ${action} user`);
    } finally {
      setActionLoading(null);
    }
  };

  const filteredUsers = users.filter(u => {
    if (roleFilter !== 'ALL' && u.role !== roleFilter) return false;
    const q = search.toLowerCase();
    return u.fullName.toLowerCase().includes(q) || 
           u.email.toLowerCase().includes(q) || 
           u.phoneNumber.includes(q);
  });

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">User Account Management</h1>
        <p className="text-sm text-slate-500 mt-1">
          Monitor platform participants, review role designations, and manage account activation states.
        </p>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-4 rounded-2xl border border-slate-200 shadow-sm">
        <div className="flex items-center space-x-1 bg-slate-100 p-1 rounded-xl">
          {(['ALL', 'CUSTOMER', 'DRIVER', 'ADMIN'] as const).map(tab => (
            <button
              key={tab}
              onClick={() => setRoleFilter(tab)}
              className={`px-4 py-2 text-xs font-semibold rounded-lg transition ${
                roleFilter === tab ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              {tab}
            </button>
          ))}
        </div>

        <div className="relative w-full sm:w-64">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
          <input
            type="text"
            placeholder="Search name, email, phone..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-slate-300 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-emerald-500"
          />
        </div>
      </div>

      {/* Users Table */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
        {loading ? (
          <div className="flex justify-center py-16">
            <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-emerald-600"></div>
          </div>
        ) : filteredUsers.length === 0 ? (
          <div className="text-center py-16 border-2 border-dashed border-slate-200 m-6 rounded-2xl">
            <Users className="w-12 h-12 text-slate-300 mx-auto mb-2" />
            <p className="text-slate-600 font-medium">No users found</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-slate-700 text-xs uppercase font-semibold">
                <tr>
                  <th className="py-3.5 px-6">User ID</th>
                  <th className="py-3.5 px-6">Full Name</th>
                  <th className="py-3.5 px-6">Email Address</th>
                  <th className="py-3.5 px-6">Phone Number</th>
                  <th className="py-3.5 px-6">Role</th>
                  <th className="py-3.5 px-6">Status</th>
                  <th className="py-3.5 px-6 text-right">Account Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredUsers.map(u => (
                  <tr key={u.id} className="hover:bg-slate-50/50">
                    <td className="py-4 px-6 font-mono text-xs font-semibold text-slate-900">
                      #{u.id}
                    </td>
                    <td className="py-4 px-6 font-semibold text-slate-900">
                      {u.fullName}
                    </td>
                    <td className="py-4 px-6 text-slate-600">
                      {u.email}
                    </td>
                    <td className="py-4 px-6 font-mono text-xs text-slate-600">
                      {u.phoneNumber}
                    </td>
                    <td className="py-4 px-6">
                      <span className={`px-2.5 py-1 text-xs rounded-full font-bold uppercase ${
                        u.role === 'ADMIN' ? 'bg-purple-100 text-purple-800' :
                        u.role === 'DRIVER' ? 'bg-blue-100 text-blue-800' :
                        'bg-emerald-100 text-emerald-800'
                      }`}>
                        {u.role}
                      </span>
                    </td>
                    <td className="py-4 px-6">
                      <span className={`px-2.5 py-1 text-xs rounded-full font-semibold ${
                        u.isActive
                          ? 'bg-emerald-100 text-emerald-800'
                          : 'bg-rose-100 text-rose-800'
                      }`}>
                        {u.isActive ? 'ACTIVE' : 'SUSPENDED'}
                      </span>
                    </td>
                    <td className="py-4 px-6 text-right">
                      {u.role !== 'ADMIN' && (
                        <button
                          onClick={() => handleToggleStatus(u)}
                          disabled={actionLoading === u.id}
                          className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
                            u.isActive
                              ? 'bg-rose-50 text-rose-700 hover:bg-rose-100'
                              : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
                          }`}
                        >
                          {u.isActive ? 'Suspend' : 'Activate'}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

