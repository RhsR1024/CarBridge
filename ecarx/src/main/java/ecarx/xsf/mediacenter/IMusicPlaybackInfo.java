package ecarx.xsf.mediacenter;

import android.app.PendingIntent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.Parcelable;

/* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
/* JADX INFO: loaded from: classes.dex */
public interface IMusicPlaybackInfo extends IInterface {
    static boolean transact(IBinder remote, int code, Parcel data, Parcel reply, int flags) {
        try { return remote.transact(code, data, reply, flags); } catch (RemoteException error) { return false; }
    }
    public static final String DESCRIPTOR = "ecarx.xsf.mediacenter.IMusicPlaybackInfo";

    /* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
    public static class Default implements IMusicPlaybackInfo {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public String getAlbum() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public String getAppIcon() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public String getAppName() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public String getArtist() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public Uri getArtwork() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public String getCurrentLyricSentence() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public int getDisplayId() {
            return 0;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public long getDuration() {
            return 0L;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public PendingIntent getLaunchIntent() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public int getLoopMode() {
            return 0;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public Uri getLyric() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public String getLyricContent() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public Uri getMediaPath() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public String getMediaType() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public Uri getNextArtwork() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public String getPackageName() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public int getPlaybackStatus() {
            return 0;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public PendingIntent getPlayerIntent() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public int getPlayingItemPositionInQueue() {
            return 0;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public String getPlayingMediaListId() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public int getPlayingMediaListType() {
            return 0;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public Uri getPreviousArtwork() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public String getRadioFrequency() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public int getRadioMode() {
            return 0;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public String getRadioStationName() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public int getSourceType() {
            return 0;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public String getTitle() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public String getUuid() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public int getVip() {
            return 0;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public boolean isCollected() {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public boolean isDownloaded() {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public boolean isSupportCollect() {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public boolean isSupportDownload() {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public boolean isSupportLoopModeSwitch() {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
        public boolean isSupportVrCtrlPlayStatus() {
            return false;
        }
    }

    /* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
    public static abstract class Stub extends Binder implements IMusicPlaybackInfo {
        static final int TRANSACTION_getAlbum = 4;
        static final int TRANSACTION_getAppIcon = 26;
        static final int TRANSACTION_getAppName = 25;
        static final int TRANSACTION_getArtist = 3;
        static final int TRANSACTION_getArtwork = 16;
        static final int TRANSACTION_getCurrentLyricSentence = 14;
        static final int TRANSACTION_getDisplayId = 34;
        static final int TRANSACTION_getDuration = 7;
        static final int TRANSACTION_getLaunchIntent = 1;
        static final int TRANSACTION_getLoopMode = 18;
        static final int TRANSACTION_getLyric = 13;
        static final int TRANSACTION_getLyricContent = 12;
        static final int TRANSACTION_getMediaPath = 10;
        static final int TRANSACTION_getMediaType = 35;
        static final int TRANSACTION_getNextArtwork = 17;
        static final int TRANSACTION_getPackageName = 27;
        static final int TRANSACTION_getPlaybackStatus = 11;
        static final int TRANSACTION_getPlayerIntent = 33;
        static final int TRANSACTION_getPlayingItemPositionInQueue = 8;
        static final int TRANSACTION_getPlayingMediaListId = 30;
        static final int TRANSACTION_getPlayingMediaListType = 32;
        static final int TRANSACTION_getPreviousArtwork = 15;
        static final int TRANSACTION_getRadioFrequency = 5;
        static final int TRANSACTION_getRadioMode = 19;
        static final int TRANSACTION_getRadioStationName = 6;
        static final int TRANSACTION_getSourceType = 9;
        static final int TRANSACTION_getTitle = 2;
        static final int TRANSACTION_getUuid = 24;
        static final int TRANSACTION_getVip = 31;
        static final int TRANSACTION_isCollected = 21;
        static final int TRANSACTION_isDownloaded = 23;
        static final int TRANSACTION_isSupportCollect = 20;
        static final int TRANSACTION_isSupportDownload = 22;
        static final int TRANSACTION_isSupportLoopModeSwitch = 28;
        static final int TRANSACTION_isSupportVrCtrlPlayStatus = 29;

        /* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
        public static class Proxy implements IMusicPlaybackInfo {
            private IBinder mRemote;

            public Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public String getAlbum() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, 4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public String getAppIcon() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_getAppIcon, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public String getAppName() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_getAppName, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public String getArtist() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, 3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public Uri getArtwork() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_getArtwork, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return (Uri) _Parcel.readTypedObject(parcelObtain2, Uri.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public String getCurrentLyricSentence() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_getCurrentLyricSentence, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public int getDisplayId() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_getDisplayId, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public long getDuration() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, 7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readLong();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            public String getInterfaceDescriptor() {
                return IMusicPlaybackInfo.DESCRIPTOR;
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public PendingIntent getLaunchIntent() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, 1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return (PendingIntent) _Parcel.readTypedObject(parcelObtain2, PendingIntent.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public int getLoopMode() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_getLoopMode, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public Uri getLyric() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_getLyric, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return (Uri) _Parcel.readTypedObject(parcelObtain2, Uri.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public String getLyricContent() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_getLyricContent, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public Uri getMediaPath() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_getMediaPath, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return (Uri) _Parcel.readTypedObject(parcelObtain2, Uri.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public String getMediaType() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_getMediaType, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public Uri getNextArtwork() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_getNextArtwork, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return (Uri) _Parcel.readTypedObject(parcelObtain2, Uri.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public String getPackageName() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_getPackageName, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public int getPlaybackStatus() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_getPlaybackStatus, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public PendingIntent getPlayerIntent() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_getPlayerIntent, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return (PendingIntent) _Parcel.readTypedObject(parcelObtain2, PendingIntent.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public int getPlayingItemPositionInQueue() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_getPlayingItemPositionInQueue, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public String getPlayingMediaListId() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_getPlayingMediaListId, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public int getPlayingMediaListType() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_getPlayingMediaListType, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public Uri getPreviousArtwork() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_getPreviousArtwork, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return (Uri) _Parcel.readTypedObject(parcelObtain2, Uri.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public String getRadioFrequency() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, 5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public int getRadioMode() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_getRadioMode, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public String getRadioStationName() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, 6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public int getSourceType() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_getSourceType, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public String getTitle() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, 2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public String getUuid() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_getUuid, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public int getVip() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_getVip, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public boolean isCollected() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_isCollected, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public boolean isDownloaded() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_isDownloaded, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public boolean isSupportCollect() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_isSupportCollect, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public boolean isSupportDownload() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_isSupportDownload, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public boolean isSupportLoopModeSwitch() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_isSupportLoopModeSwitch, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicPlaybackInfo
            public boolean isSupportVrCtrlPlayStatus() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicPlaybackInfo.DESCRIPTOR);
                    IMusicPlaybackInfo.transact(this.mRemote, Stub.TRANSACTION_isSupportVrCtrlPlayStatus, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, IMusicPlaybackInfo.DESCRIPTOR);
        }

        public static IMusicPlaybackInfo asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(IMusicPlaybackInfo.DESCRIPTOR);
            return (iInterfaceQueryLocalInterface == null || !(iInterfaceQueryLocalInterface instanceof IMusicPlaybackInfo)) ? new Proxy(iBinder) : (IMusicPlaybackInfo) iInterfaceQueryLocalInterface;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface(IMusicPlaybackInfo.DESCRIPTOR);
            }
            if (i == 1598968902) {
                parcel2.writeString(IMusicPlaybackInfo.DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    PendingIntent launchIntent = getLaunchIntent();
                    parcel2.writeNoException();
                    _Parcel.writeTypedObject(parcel2, launchIntent, 1);
                    return true;
                case 2:
                    String title = getTitle();
                    parcel2.writeNoException();
                    parcel2.writeString(title);
                    return true;
                case 3:
                    String artist = getArtist();
                    parcel2.writeNoException();
                    parcel2.writeString(artist);
                    return true;
                case 4:
                    String album = getAlbum();
                    parcel2.writeNoException();
                    parcel2.writeString(album);
                    return true;
                case 5:
                    String radioFrequency = getRadioFrequency();
                    parcel2.writeNoException();
                    parcel2.writeString(radioFrequency);
                    return true;
                case 6:
                    String radioStationName = getRadioStationName();
                    parcel2.writeNoException();
                    parcel2.writeString(radioStationName);
                    return true;
                case 7:
                    long duration = getDuration();
                    parcel2.writeNoException();
                    parcel2.writeLong(duration);
                    return true;
                case TRANSACTION_getPlayingItemPositionInQueue /* 8 */:
                    int playingItemPositionInQueue = getPlayingItemPositionInQueue();
                    parcel2.writeNoException();
                    parcel2.writeInt(playingItemPositionInQueue);
                    return true;
                case TRANSACTION_getSourceType /* 9 */:
                    int sourceType = getSourceType();
                    parcel2.writeNoException();
                    parcel2.writeInt(sourceType);
                    return true;
                case TRANSACTION_getMediaPath /* 10 */:
                    Uri mediaPath = getMediaPath();
                    parcel2.writeNoException();
                    _Parcel.writeTypedObject(parcel2, mediaPath, 1);
                    return true;
                case TRANSACTION_getPlaybackStatus /* 11 */:
                    int playbackStatus = getPlaybackStatus();
                    parcel2.writeNoException();
                    parcel2.writeInt(playbackStatus);
                    return true;
                case TRANSACTION_getLyricContent /* 12 */:
                    String lyricContent = getLyricContent();
                    parcel2.writeNoException();
                    parcel2.writeString(lyricContent);
                    return true;
                case TRANSACTION_getLyric /* 13 */:
                    Uri lyric = getLyric();
                    parcel2.writeNoException();
                    _Parcel.writeTypedObject(parcel2, lyric, 1);
                    return true;
                case TRANSACTION_getCurrentLyricSentence /* 14 */:
                    String currentLyricSentence = getCurrentLyricSentence();
                    parcel2.writeNoException();
                    parcel2.writeString(currentLyricSentence);
                    return true;
                case TRANSACTION_getPreviousArtwork /* 15 */:
                    Uri previousArtwork = getPreviousArtwork();
                    parcel2.writeNoException();
                    _Parcel.writeTypedObject(parcel2, previousArtwork, 1);
                    return true;
                case TRANSACTION_getArtwork /* 16 */:
                    Uri artwork = getArtwork();
                    parcel2.writeNoException();
                    _Parcel.writeTypedObject(parcel2, artwork, 1);
                    return true;
                case TRANSACTION_getNextArtwork /* 17 */:
                    Uri nextArtwork = getNextArtwork();
                    parcel2.writeNoException();
                    _Parcel.writeTypedObject(parcel2, nextArtwork, 1);
                    return true;
                case TRANSACTION_getLoopMode /* 18 */:
                    int loopMode = getLoopMode();
                    parcel2.writeNoException();
                    parcel2.writeInt(loopMode);
                    return true;
                case TRANSACTION_getRadioMode /* 19 */:
                    int radioMode = getRadioMode();
                    parcel2.writeNoException();
                    parcel2.writeInt(radioMode);
                    return true;
                case TRANSACTION_isSupportCollect /* 20 */:
                    boolean zIsSupportCollect = isSupportCollect();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsSupportCollect ? 1 : 0);
                    return true;
                case TRANSACTION_isCollected /* 21 */:
                    boolean zIsCollected = isCollected();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsCollected ? 1 : 0);
                    return true;
                case TRANSACTION_isSupportDownload /* 22 */:
                    boolean zIsSupportDownload = isSupportDownload();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsSupportDownload ? 1 : 0);
                    return true;
                case TRANSACTION_isDownloaded /* 23 */:
                    boolean zIsDownloaded = isDownloaded();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsDownloaded ? 1 : 0);
                    return true;
                case TRANSACTION_getUuid /* 24 */:
                    String uuid = getUuid();
                    parcel2.writeNoException();
                    parcel2.writeString(uuid);
                    return true;
                case TRANSACTION_getAppName /* 25 */:
                    String appName = getAppName();
                    parcel2.writeNoException();
                    parcel2.writeString(appName);
                    return true;
                case TRANSACTION_getAppIcon /* 26 */:
                    String appIcon = getAppIcon();
                    parcel2.writeNoException();
                    parcel2.writeString(appIcon);
                    return true;
                case TRANSACTION_getPackageName /* 27 */:
                    String packageName = getPackageName();
                    parcel2.writeNoException();
                    parcel2.writeString(packageName);
                    return true;
                case TRANSACTION_isSupportLoopModeSwitch /* 28 */:
                    boolean zIsSupportLoopModeSwitch = isSupportLoopModeSwitch();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsSupportLoopModeSwitch ? 1 : 0);
                    return true;
                case TRANSACTION_isSupportVrCtrlPlayStatus /* 29 */:
                    boolean zIsSupportVrCtrlPlayStatus = isSupportVrCtrlPlayStatus();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsSupportVrCtrlPlayStatus ? 1 : 0);
                    return true;
                case TRANSACTION_getPlayingMediaListId /* 30 */:
                    String playingMediaListId = getPlayingMediaListId();
                    parcel2.writeNoException();
                    parcel2.writeString(playingMediaListId);
                    return true;
                case TRANSACTION_getVip /* 31 */:
                    int vip = getVip();
                    parcel2.writeNoException();
                    parcel2.writeInt(vip);
                    return true;
                case TRANSACTION_getPlayingMediaListType /* 32 */:
                    int playingMediaListType = getPlayingMediaListType();
                    parcel2.writeNoException();
                    parcel2.writeInt(playingMediaListType);
                    return true;
                case TRANSACTION_getPlayerIntent /* 33 */:
                    PendingIntent playerIntent = getPlayerIntent();
                    parcel2.writeNoException();
                    _Parcel.writeTypedObject(parcel2, playerIntent, 1);
                    return true;
                case TRANSACTION_getDisplayId /* 34 */:
                    int displayId = getDisplayId();
                    parcel2.writeNoException();
                    parcel2.writeInt(displayId);
                    return true;
                case TRANSACTION_getMediaType /* 35 */:
                    String mediaType = getMediaType();
                    parcel2.writeNoException();
                    parcel2.writeString(mediaType);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }
    }

    /* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
    public static class _Parcel {
        /* JADX INFO: Access modifiers changed from: private */
        public static <T> T readTypedObject(Parcel parcel, Parcelable.Creator<T> creator) {
            if (parcel.readInt() != 0) {
                return creator.createFromParcel(parcel);
            }
            return null;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static <T extends Parcelable> void writeTypedObject(Parcel parcel, T t, int i) {
            if (t == null) {
                parcel.writeInt(0);
            } else {
                parcel.writeInt(1);
                t.writeToParcel(parcel, i);
            }
        }
    }

    String getAlbum();

    String getAppIcon();

    String getAppName();

    String getArtist();

    Uri getArtwork();

    String getCurrentLyricSentence();

    int getDisplayId();

    long getDuration();

    PendingIntent getLaunchIntent();

    int getLoopMode();

    Uri getLyric();

    String getLyricContent();

    Uri getMediaPath();

    String getMediaType();

    Uri getNextArtwork();

    String getPackageName();

    int getPlaybackStatus();

    PendingIntent getPlayerIntent();

    int getPlayingItemPositionInQueue();

    String getPlayingMediaListId();

    int getPlayingMediaListType();

    Uri getPreviousArtwork();

    String getRadioFrequency();

    int getRadioMode();

    String getRadioStationName();

    int getSourceType();

    String getTitle();

    String getUuid();

    int getVip();

    boolean isCollected();

    boolean isDownloaded();

    boolean isSupportCollect();

    boolean isSupportDownload();

    boolean isSupportLoopModeSwitch();

    boolean isSupportVrCtrlPlayStatus();
}



