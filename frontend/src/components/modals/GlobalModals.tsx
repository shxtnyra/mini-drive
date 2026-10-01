import CreateFolderModal from './CreateFolderModal';
import EmailVerificationModal from './EmailVerificationModal';
import RenameModal from './RenameModal';


export default function GlobalModals() {
  return (
    <>
      <RenameModal />
      <CreateFolderModal/>
      <EmailVerificationModal/>
    </>
  );
}