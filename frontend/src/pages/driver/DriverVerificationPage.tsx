import React, { useState, useEffect } from 'react';
import { driverApi } from '../../services/api';
import { DriverProfile, DriverDocument } from '../../types';
import { 
  ShieldCheck, 
  AlertCircle, 
  Upload, 
  FileText, 
  CheckCircle2, 
  Clock, 
  XCircle, 
  Calendar,
  Lock
} from 'lucide-react';

export const DriverVerificationPage: React.FC = () => {
  const [profile, setProfile] = useState<DriverProfile | null>(null);
  const [documents, setDocuments] = useState<DriverDocument[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [selectedType, setSelectedType] = useState('DRIVING_LICENCE');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadSuccess, setUploadSuccess] = useState<string | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);

  const fetchKycData = async () => {
    try {
      setLoading(true);
      const [pRes, dRes] = await Promise.all([
        driverApi.getProfile(),
        driverApi.getDocuments()
      ]);
      setProfile(pRes.data);
      setDocuments(dRes.data);
    } catch (err) {
      console.error('Failed to load KYC information', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchKycData();
  }, []);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
    }
  };

  const handleUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedFile) {
      setUploadError('Please select a document or image file to upload.');
      return;
    }

    try {
      setUploading(true);
      setUploadError(null);
      setUploadSuccess(null);

      await driverApi.uploadDocument(selectedType, selectedFile);
      setUploadSuccess(`Successfully uploaded ${selectedType.replace(/_/g, ' ')}! Our admin team will review it.`);
      setSelectedFile(null);
      await fetchKycData();
    } catch (err: any) {
      setUploadError(err.response?.data?.message || 'Failed to upload document');
    } finally {
      setUploading(false);
    }
  };

  if (loading && !profile) {
    return (
      <div className="flex justify-center items-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-emerald-600"></div>
      </div>
    );
  }

  const status = profile?.verificationStatus || 'PENDING';

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Driver Verification & KYC</h1>
        <p className="text-sm text-slate-500 mt-1">
          Upload and manage your professional driving credentials to receive customer bookings.
        </p>
      </div>

      {/* KYC Status Card */}
      <div className={`rounded-2xl p-6 border ${
        status === 'VERIFIED'
          ? 'bg-emerald-50 border-emerald-200'
          : status === 'REJECTED'
          ? 'bg-rose-50 border-rose-200'
          : 'bg-amber-50 border-amber-200'
      }`}>
        <div className="flex items-start space-x-4">
          <div className="p-3 bg-white rounded-xl shadow-sm shrink-0">
            {status === 'VERIFIED' ? (
              <ShieldCheck className="w-8 h-8 text-emerald-600" />
            ) : status === 'REJECTED' ? (
              <XCircle className="w-8 h-8 text-rose-600" />
            ) : (
              <Clock className="w-8 h-8 text-amber-600" />
            )}
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <h2 className="text-lg font-bold text-slate-900">
                Verification Status: {status}
              </h2>
            </div>
            <p className="text-sm text-slate-700 mt-1">
              {status === 'VERIFIED' && 'Your account and commercial driving documents have been verified by the DRIVIO Operations team. You are authorized to accept bookings.'}
              {status === 'PENDING' && 'Your documents are currently undergoing review. Admin approvals typically take 2-4 hours. You will receive real-time notifications upon approval.'}
              {status === 'REJECTED' && 'Your verification could not be approved. Please review your documents for legibility or expiration and upload an updated copy below.'}
            </p>
            <div className="mt-3 flex items-center text-xs text-slate-500 space-x-1">
              <Lock className="w-3.5 h-3.5 text-slate-400" />
              <span>All documents are securely encrypted and protected per data privacy guidelines.</span>
            </div>
          </div>
        </div>
      </div>

      {/* Document Upload Form */}
      <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200">
        <h2 className="text-lg font-bold text-slate-900 mb-4 flex items-center">
          <Upload className="w-5 h-5 mr-2 text-emerald-600" />
          Upload New Document
        </h2>

        {uploadSuccess && (
          <div className="mb-4 p-4 bg-emerald-50 text-emerald-800 border border-emerald-200 rounded-xl text-sm flex items-center">
            <CheckCircle2 className="w-4 h-4 mr-2 shrink-0" />
            {uploadSuccess}
          </div>
        )}

        {uploadError && (
          <div className="mb-4 p-4 bg-rose-50 text-rose-800 border border-rose-200 rounded-xl text-sm flex items-center">
            <AlertCircle className="w-4 h-4 mr-2 shrink-0" />
            {uploadError}
          </div>
        )}

        <form onSubmit={handleUpload} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Document Type
              </label>
              <select
                value={selectedType}
                onChange={e => setSelectedType(e.target.value)}
                className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
              >
                <option value="DRIVING_LICENCE">Commercial Driving Licence</option>
                <option value="PROFILE_PHOTO">Driver Profile Photo</option>
                <option value="ID_PROOF">Government Identity Proof (Aadhaar / Voter ID)</option>
                <option value="POLICE_VERIFICATION">Police Clearance Certificate</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Select File (PDF, PNG, JPG)
              </label>
              <input
                type="file"
                accept=".pdf,image/png,image/jpeg,image/webp"
                onChange={handleFileChange}
                className="w-full text-sm text-slate-500 file:mr-4 file:py-2 file:px-4 file:rounded-xl file:border-0 file:text-xs file:font-semibold file:bg-emerald-50 file:text-emerald-700 hover:file:bg-emerald-100"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={uploading || !selectedFile}
            className={`w-full sm:w-auto px-6 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold rounded-xl text-sm transition shadow ${
              uploading || !selectedFile ? 'opacity-50 cursor-not-allowed' : ''
            }`}
          >
            {uploading ? 'Uploading...' : 'Submit Document for Verification'}
          </button>
        </form>
      </div>

      {/* Uploaded Documents List */}
      <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200">
        <h2 className="text-lg font-bold text-slate-900 mb-4 flex items-center">
          <FileText className="w-5 h-5 mr-2 text-slate-700" />
          Submitted Documents ({documents.length})
        </h2>

        {documents.length === 0 ? (
          <div className="text-center py-8 border-2 border-dashed border-slate-200 rounded-xl text-slate-500 text-sm">
            No verification documents uploaded yet.
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {documents.map(doc => (
              <div key={doc.id} className="py-4 flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <div className="p-2.5 bg-slate-100 text-slate-600 rounded-xl">
                    <FileText className="w-5 h-5" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-slate-900 text-sm">
                      {doc.documentType.replace(/_/g, ' ')}
                    </h3>
                    <div className="flex items-center space-x-3 text-xs text-slate-400 mt-0.5">
                      <span>{doc.fileName}</span>
                      <span>•</span>
                      <span className="flex items-center">
                        <Calendar className="w-3 h-3 mr-1" />
                        {new Date(doc.createdAt).toLocaleDateString()}
                      </span>
                    </div>
                  </div>
                </div>

                <span className={`px-2.5 py-1 text-xs rounded-full font-semibold ${
                  doc.verificationStatus === 'VERIFIED'
                    ? 'bg-emerald-100 text-emerald-800'
                    : doc.verificationStatus === 'REJECTED'
                    ? 'bg-rose-100 text-rose-800'
                    : 'bg-amber-100 text-amber-800'
                }`}>
                  {doc.verificationStatus}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

