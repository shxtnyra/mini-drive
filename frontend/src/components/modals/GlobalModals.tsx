import CreateFolderModal from './CreateFolderModal';
import EmailVerificationModal from './EmailVerificationModal';
import RenameModal from './RenameModal';
import MoveItemsModal from './MoveItemsModal';
import ShareModal from './ShareModal';
import { useUIStore } from '../../store/useUIStore';

export default function GlobalModals() {
  const isMoveFilesOpen = useUIStore((state) => state.isMoveFilesOpen);
  const isShareOpen = useUIStore((state) => state.isShareOpen);
  const shareTarget = useUIStore((state) => state.shareTarget);

  return (
    <>
      <RenameModal />
      <CreateFolderModal/>
      {isMoveFilesOpen && <MoveItemsModal />}
      {isShareOpen && shareTarget && <ShareModal key={shareTarget.uuid} />}
      <EmailVerificationModal/>
    </>
  );
}