import { useEffect, useMemo, useState } from 'react';
import { Check, Copy, Link2, Search, UserRound, UsersRound, X } from 'lucide-react';
import { shareService } from '../../api/shareService';
import { useUIStore } from '../../store/useUIStore';
import type {
  ShareDetails,
  SharePermission,
  ShareTargetsResponse,
} from '../../types/api';
import { getApiErrorMessage } from '../../utils/apiErrors';
import Modal from './Modal';

type ShareTarget =
  | { type: 'USER'; uuid: string; name: string; detail: string }
  | { type: 'GROUP'; uuid: string; name: string; detail: string };

const permissionLabels: Record<SharePermission, string> = {
  READ: 'Читатель',
  WRITE: 'Редактор',
  ALL: 'Полный доступ',
};

export default function ShareModal() {
  const isOpen = useUIStore((state) => state.isShareOpen);
  const target = useUIStore((state) => state.shareTarget);
  const closeModal = useUIStore((state) => state.closeShareModal);

  const [details, setDetails] = useState<ShareDetails | null>(null);
  const [searchResult, setSearchResult] = useState<{
    query: string;
    results: ShareTargetsResponse;
  } | null>(null);
  const [query, setQuery] = useState('');
  const [selectedTargets, setSelectedTargets] = useState<ShareTarget[]>([]);
  const [permission, setPermission] = useState<SharePermission>('READ');
  const [isSaving, setIsSaving] = useState(false);
  const [isUpdatingLink, setIsUpdatingLink] = useState(false);
  const [copied, setCopied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const isLoading = details === null && error === null;
  const normalizedQuery = query.trim();
  const searchResults = searchResult?.query === normalizedQuery
    ? searchResult.results
    : { users: [], groups: [] };
  const hasCurrentSearchResults = searchResult?.query === normalizedQuery;

  useEffect(() => {
    if (!isOpen || !target) return;
    let isCurrent = true;

    shareService.getShares(target.uuid)
      .then((response) => {
        if (isCurrent) setDetails(response);
      })
      .catch((requestError: unknown) => {
        if (isCurrent) setError(getApiErrorMessage(requestError, 'Не удалось загрузить настройки доступа'));
      });

    return () => {
      isCurrent = false;
    };
  }, [isOpen, target]);

  useEffect(() => {
    if (!isOpen || normalizedQuery.length < 3) {
      return;
    }

    let isCurrent = true;
    const timeoutId = window.setTimeout(() => {
      shareService.searchTargets(normalizedQuery)
        .then((results) => {
          if (isCurrent) setSearchResult({ query: normalizedQuery, results });
        })
        .catch((requestError: unknown) => {
          if (isCurrent) setError(getApiErrorMessage(requestError, 'Не удалось выполнить поиск'));
        });
    }, 250);

    return () => {
      isCurrent = false;
      window.clearTimeout(timeoutId);
    };
  }, [isOpen, normalizedQuery]);

  const existingTargetKeys = useMemo(
    () => new Set((details?.recipients ?? []).map((recipient) => `${recipient.type}:${recipient.recipientUuid}`)),
    [details]
  );
  const selectedTargetKeys = useMemo(
    () => new Set(selectedTargets.map((item) => `${item.type}:${item.uuid}`)),
    [selectedTargets]
  );

  const addTarget = (nextTarget: ShareTarget) => {
    setSelectedTargets((current) => (
      current.some((item) => item.type === nextTarget.type && item.uuid === nextTarget.uuid)
        ? current
        : [...current, nextTarget]
    ));
  };

  const removeTarget = (targetToRemove: ShareTarget) => {
    setSelectedTargets((current) => current.filter(
      (item) => item.type !== targetToRemove.type || item.uuid !== targetToRemove.uuid
    ));
  };

  const handleAddRecipients = async () => {
    if (!target || selectedTargets.length === 0 || isSaving) return;
    try {
      setIsSaving(true);
      setError(null);
      const updated = await shareService.addRecipients(
        target.uuid,
        selectedTargets.filter((item) => item.type === 'USER').map((item) => item.uuid),
        selectedTargets.filter((item) => item.type === 'GROUP').map((item) => item.uuid),
        permission
      );
      setDetails(updated);
      setSelectedTargets([]);
      setQuery('');
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Не удалось выдать доступ'));
    } finally {
      setIsSaving(false);
    }
  };

  const handleRevoke = async (shareUuid: string) => {
    try {
      setError(null);
      await shareService.revokeShare(shareUuid);
      if (target) setDetails(await shareService.getShares(target.uuid));
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Не удалось отозвать доступ'));
    }
  };

  const handleLinkToggle = async () => {
    if (!target || !details || isUpdatingLink) return;
    try {
      setIsUpdatingLink(true);
      setError(null);
      setDetails(await shareService.setPublicLink(target.uuid, !details.link.enabled));
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Не удалось изменить доступ по ссылке'));
    } finally {
      setIsUpdatingLink(false);
    }
  };

  const publicUrl = details?.link.token
    ? `${window.location.origin}/share/${details.link.token}`
    : null;

  const handleCopyLink = async () => {
    if (!publicUrl) return;
    try {
      await navigator.clipboard.writeText(publicUrl);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1800);
    } catch {
      setError('Не удалось скопировать ссылку. Скопируйте её вручную.');
    }
  };

  const displayName = target?.filename ?? 'элемент';

  return (
    <Modal isOpen={isOpen} onClose={closeModal}>
      <div className="flex items-center justify-between mb-5">
        <div className="min-w-0">
          <h2 id="modal-title" className="text-3xl font-light text-primary tracking-tight">
            Поделиться
          </h2>
          <p className="text-sm text-secondary truncate mt-1" title={displayName}>{displayName}</p>
        </div>
        <button
          onClick={closeModal}
          className="text-secondary hover:text-primary transition-colors cursor-pointer"
          aria-label="Закрыть"
        >
          <X size={20} />
        </button>
      </div>

      {isLoading ? (
        <p className="py-8 text-center text-sm text-secondary">Загрузка настроек доступа...</p>
      ) : (
        <div className="space-y-5">
          <section>
            <label className="block text-secondary text-xs mb-2 uppercase tracking-wider">
              Добавить пользователей или группы
            </label>
            <div className="relative">
              <Search size={17} className="absolute left-3 top-3 text-subtle" />
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Введите имя или название группы"
                className="w-full bg-transparent border border-border rounded-lg text-primary pl-10 pr-3 py-2.5 text-sm focus:outline-none focus:border-border-focus placeholder-placeholder"
              />
            </div>
            {normalizedQuery.length > 0 && normalizedQuery.length < 3 && (
              <p className="text-xs text-secondary mt-2">Для поиска введите минимум 3 символа.</p>
            )}
            {normalizedQuery.length >= 3 && (
              <div className="mt-2 max-h-36 overflow-y-auto rounded-lg border border-border divide-y divide-border">
                {searchResults.users
                  .filter((user) => !existingTargetKeys.has(`USER:${user.uuid}`))
                  .map((user) => {
                    const item: ShareTarget = {
                      type: 'USER',
                      uuid: user.uuid,
                      name: user.displayName || user.username,
                      detail: user.email,
                    };
                    const selected = selectedTargetKeys.has(`USER:${user.uuid}`);
                    return (
                      <button
                        key={`user-${user.uuid}`}
                        onClick={() => selected ? removeTarget(item) : addTarget(item)}
                        className="w-full flex items-center gap-3 px-3 py-2.5 text-left hover:bg-sidebar-hover cursor-pointer"
                      >
                        <UserRound size={17} className="text-secondary shrink-0" />
                        <span className="flex-1 min-w-0">
                          <span className="block text-sm text-primary truncate">{item.name}</span>
                          <span className="block text-xs text-secondary truncate">{item.detail}</span>
                        </span>
                        {selected && <Check size={17} className="text-primary" />}
                      </button>
                    );
                  })}
                {searchResults.groups
                  .filter((group) => !existingTargetKeys.has(`GROUP:${group.uuid}`))
                  .map((group) => {
                    const item: ShareTarget = {
                      type: 'GROUP',
                      uuid: group.uuid,
                      name: group.name,
                      detail: `${group.membersCount} участников`,
                    };
                    const selected = selectedTargetKeys.has(`GROUP:${group.uuid}`);
                    return (
                      <button
                        key={`group-${group.uuid}`}
                        onClick={() => selected ? removeTarget(item) : addTarget(item)}
                        className="w-full flex items-center gap-3 px-3 py-2.5 text-left hover:bg-sidebar-hover cursor-pointer"
                      >
                        <UsersRound size={17} className="text-secondary shrink-0" />
                        <span className="flex-1 min-w-0">
                          <span className="block text-sm text-primary truncate">{item.name}</span>
                          <span className="block text-xs text-secondary truncate">{item.detail}</span>
                        </span>
                        {selected && <Check size={17} className="text-primary" />}
                      </button>
                    );
                  })}
                {searchResults.users.length === 0 && searchResults.groups.length === 0 && (
                  <p className="px-3 py-4 text-center text-xs text-secondary">
                    {hasCurrentSearchResults ? 'Ничего не найдено' : 'Поиск...'}
                  </p>
                )}
              </div>
            )}

            {selectedTargets.length > 0 && (
              <div className="mt-3 flex flex-wrap gap-2">
                {selectedTargets.map((item) => (
                  <button
                    key={`${item.type}:${item.uuid}`}
                    onClick={() => removeTarget(item)}
                    className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-full bg-selection-bg text-xs text-primary cursor-pointer"
                    title="Убрать из списка"
                  >
                    <span className="max-w-44 truncate">{item.name}</span>
                    <X size={13} />
                  </button>
                ))}
              </div>
            )}

            {selectedTargets.length > 0 && (
              <div className="flex items-center gap-3 mt-3">
                <select
                  value={permission}
                  onChange={(event) => setPermission(event.target.value as SharePermission)}
                  className="flex-1 bg-surface border border-border rounded-lg px-3 py-2 text-sm text-primary"
                >
                  {Object.entries(permissionLabels).map(([value, label]) => (
                    <option key={value} value={value}>{label}</option>
                  ))}
                </select>
                <button
                  onClick={handleAddRecipients}
                  disabled={isSaving}
                  className="px-4 py-2 text-sm font-semibold bg-btn text-btn-text hover:bg-btn-hover rounded-lg disabled:opacity-50 cursor-pointer"
                >
                  {isSaving ? 'Сохранение...' : 'Выдать доступ'}
                </button>
              </div>
            )}
          </section>

          <section className="border-t border-border pt-4">
            <div className="flex items-center justify-between gap-3">
              <div className="flex items-center gap-3 min-w-0">
                <Link2 size={18} className="text-secondary shrink-0" />
                <div>
                  <p className="text-sm font-medium text-primary">Доступ по ссылке</p>
                  <p className="text-xs text-secondary">
                    {details?.link.enabled ? 'Все, у кого есть ссылка, могут просматривать' : 'Доступ выключен'}
                  </p>
                </div>
              </div>
              <button
                role="switch"
                aria-checked={details?.link.enabled ?? false}
                onClick={handleLinkToggle}
                disabled={isUpdatingLink || !details}
                className={`relative w-11 h-6 rounded-full transition-colors cursor-pointer disabled:opacity-50 flex items-start ${
                  details?.link.enabled ? 'bg-selection-border' : 'bg-border'
                }`}
              >
                <span className={`absolute top-1 w-4 h-4 rounded-full bg-white transition-transform ${
                  details?.link.enabled ? 'translate-x-6' : 'translate-x-1'
                }`} />
              </button>
            </div>
            {publicUrl && (
              <div className="flex items-center gap-2 mt-3">
                <input
                  readOnly
                  value={publicUrl}
                  className="min-w-0 flex-1 bg-page border border-border rounded-lg px-3 py-2 text-xs text-secondary"
                />
                <button
                  onClick={handleCopyLink}
                  className="p-2 rounded-lg border border-border text-secondary hover:text-primary hover:bg-sidebar-hover cursor-pointer"
                  title="Скопировать ссылку"
                >
                  {copied ? <Check size={17} /> : <Copy size={17} />}
                </button>
              </div>
            )}
          </section>

          {details && details.recipients.length > 0 && (
            <section className="border-t border-border pt-4">
              <p className="text-secondary text-xs mb-2 uppercase tracking-wider">У кого есть доступ</p>
              <div className="max-h-32 overflow-y-auto divide-y divide-border">
                {details.recipients.map((recipient) => (
                  <div key={recipient.shareUuid} className="flex items-center gap-3 py-2">
                    {recipient.type === 'USER'
                      ? <UserRound size={17} className="text-secondary shrink-0" />
                      : <UsersRound size={17} className="text-secondary shrink-0" />}
                    <div className="flex-1 min-w-0">
                      <p className="text-sm text-primary truncate">{recipient.name}</p>
                      <p className="text-xs text-secondary">
                        {recipient.email || recipient.type} · {permissionLabels[recipient.permission]}
                      </p>
                    </div>
                    <button
                      onClick={() => handleRevoke(recipient.shareUuid)}
                      className="p-1.5 text-secondary hover:text-error-text cursor-pointer"
                      title="Отозвать доступ"
                    >
                      <X size={16} />
                    </button>
                  </div>
                ))}
              </div>
            </section>
          )}
        </div>
      )}

      {error && <p className="text-xs text-error-text mt-3">{error}</p>}
    </Modal>
  );
}
