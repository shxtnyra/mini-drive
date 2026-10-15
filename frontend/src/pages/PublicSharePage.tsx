import { useEffect, useState } from 'react';
import { ArrowLeft, Download, File, Folder, Loader, TriangleAlert } from 'lucide-react';
import { useParams } from 'react-router-dom';
import { shareService } from '../api/shareService';
import type { NodeResponse, PublicNodeResponse } from '../types/api';
import { formatBytes } from '../utils/formatters';
import { getApiErrorMessage } from '../utils/apiErrors';

export default function PublicSharePage() {
  const { token = '' } = useParams<{ token: string }>();
  const [view, setView] = useState<{
    token: string;
    current: PublicNodeResponse | null;
    history: PublicNodeResponse[];
    error: string | null;
  } | null>(null);
  const [isDownloading, setIsDownloading] = useState(false);
  const isLoading = view?.token !== token;
  const current = view?.token === token ? view.current : null;
  const history = view?.token === token ? view.history : [];
  const error = view?.token === token ? view.error : null;

  useEffect(() => {
    let isCurrent = true;
    shareService.getPublicNode(token)
      .then((response) => {
        if (isCurrent) {
          setView({ token, current: response, history: [], error: null });
        }
      })
      .catch((requestError: unknown) => {
        if (isCurrent) {
          setView({
            token,
            current: null,
            history: [],
            error: getApiErrorMessage(requestError, 'Ссылка недействительна или доступ отозван'),
          });
        }
      });

    return () => {
      isCurrent = false;
    };
  }, [token]);

  const openFolder = async (folder: NodeResponse) => {
    try {
      const response = await shareService.getPublicNode(token, folder.uuid);
      setView((currentView) => (
        currentView?.token === token && currentView.current
          ? {
              ...currentView,
              current: response,
              history: [...currentView.history, currentView.current],
              error: null,
            }
          : currentView
      ));
    } catch (requestError) {
      setView((currentView) => currentView?.token === token
        ? { ...currentView, error: getApiErrorMessage(requestError, 'Не удалось открыть папку') }
        : currentView);
    }
  };

  const downloadFile = async (file: NodeResponse) => {
    try {
      setIsDownloading(true);
      const blob = await shareService.downloadPublicFile(token, file.uuid);
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = file.filename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (requestError) {
      setView((currentView) => currentView?.token === token
        ? { ...currentView, error: getApiErrorMessage(requestError, 'Не удалось скачать файл') }
        : currentView);
    } finally {
      setIsDownloading(false);
    }
  };

  const goBack = () => {
    const parent = history[history.length - 1];
    if (!parent) return;
    setView((currentView) => currentView?.token === token
      ? {
          ...currentView,
          current: parent,
          history: currentView.history.slice(0, -1),
          error: null,
        }
      : currentView);
  };

  const node = current?.node;

  return (
    <main className="min-h-screen bg-page text-primary px-4 py-10">
      <section className="mx-auto max-w-3xl bg-surface border border-border rounded-xl shadow-lg overflow-hidden">
        <header className="px-6 py-5 border-b border-border">
          <p className="text-xs uppercase tracking-wider text-secondary">Mini Drive · общий доступ</p>
          <div className="flex items-center gap-3 mt-3">
            {history.length > 0 && (
              <button
                onClick={goBack}
                className="p-2 -ml-2 rounded-lg text-secondary hover:bg-sidebar-hover hover:text-primary cursor-pointer"
                aria-label="Назад"
              >
                <ArrowLeft size={19} />
              </button>
            )}
            <h1 className="text-2xl font-light truncate">{node?.filename ?? 'Общий доступ'}</h1>
          </div>
        </header>

        {isLoading ? (
          <div className="flex items-center justify-center gap-2 p-12 text-sm text-secondary">
            <Loader size={18} className="animate-spin" />
            Загрузка...
          </div>
        ) : error && !node ? (
          <div className="flex items-center gap-3 p-8 text-sm text-error-text">
            <TriangleAlert size={20} />
            {error}
          </div>
        ) : node?.fileType === 'FILE' ? (
          <div className="flex flex-col items-center gap-4 p-12">
            <File size={48} className="text-secondary" />
            <p className="text-sm text-secondary">{formatBytes(node.sizeBytes)}</p>
            <button
              onClick={() => downloadFile(node)}
              disabled={isDownloading}
              className="inline-flex items-center gap-2 px-5 py-2.5 bg-btn text-btn-text hover:bg-btn-hover rounded-lg text-sm font-semibold disabled:opacity-50 cursor-pointer"
            >
              <Download size={17} />
              {isDownloading ? 'Подготовка...' : 'Скачать'}
            </button>
          </div>
        ) : (
          <div className="divide-y divide-border">
            {current?.children.length ? current.children.map((child) => (
              <PublicItem
                key={child.uuid}
                item={child}
                onOpen={() => child.fileType === 'FOLDER' ? openFolder(child) : downloadFile(child)}
              />
            )) : (
              <p className="p-10 text-center text-sm text-secondary">Папка пуста</p>
            )}
          </div>
        )}

        {error && node && (
          <p className="px-6 py-3 text-sm text-error-text border-t border-border">{error}</p>
        )}
      </section>
    </main>
  );
}

function PublicItem({ item, onOpen }: { item: NodeResponse; onOpen: () => void }) {
  const isFolder = item.fileType === 'FOLDER';
  return (
    <button
      onClick={onOpen}
      className="w-full flex items-center gap-4 px-6 py-4 text-left hover:bg-sidebar-hover transition-colors cursor-pointer"
    >
      {isFolder
        ? <Folder size={20} className="text-secondary shrink-0" />
        : <File size={20} className="text-secondary shrink-0" />}
      <span className="flex-1 min-w-0 text-sm text-primary truncate">{item.filename}</span>
      <span className="text-xs text-secondary">
        {isFolder ? 'Папка' : formatBytes(item.sizeBytes)}
      </span>
      {isFolder ? null : <Download size={17} className="text-secondary shrink-0" />}
    </button>
  );
}
