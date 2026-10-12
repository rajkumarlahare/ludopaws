import fs from 'node:fs';

const files = {
  quickChat: 'android/app/src/main/java/com/ludoproof/game/feature/settings/presentation/QuickChatUi.kt',
  settingsKit: 'android/app/src/main/java/com/ludoproof/game/feature/settings/presentation/SettingsVisualKit.kt',
  settingsUi: 'android/app/src/main/java/com/ludoproof/game/feature/settings/presentation/SettingsDialogUi.kt',
  offlineRail: 'android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/gameplay/OfflinePlayerRailUi.kt',
  onlinePresentation: 'android/app/src/main/java/com/ludoproof/game/feature/online/presentation/OnlineLudoPawsPresentation.kt',
  reactionPolicy: 'android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/gameplay/OfflineQuickReactionPolicy.kt',
  settingsEntry: 'android/app/src/main/java/com/ludoproof/game/core/ui/dialogs/ArcadeDialogs.kt',
};

for (const [label, file] of Object.entries(files)) {
  if (!fs.existsSync(file)) throw new Error(`Quick Chat/settings gate: missing ${label}: ${file}`);
}
const read = label => fs.readFileSync(files[label], 'utf8');

const quickChat = read('quickChat');
const emojiBlock = quickChat.match(/val EMOJIS: List<String> = listOf\(([\s\S]*?)\n    \)/);
if (!emojiBlock) throw new Error('Quick Chat emoji catalog is missing.');
const emojis = [...emojiBlock[1].matchAll(/"([^"]+)"/g)].map(match => match[1]);
if (emojis.length !== 24 || new Set(emojis).size !== 24) {
  throw new Error(`Quick Chat must contain 24 unique emojis; found ${emojis.length}.`);
}
for (const marker of ['class QuickChatButtonView', 'fun showQuickChatPopup(', 'fun animateQuickChatReaction(']) {
  if (!quickChat.includes(marker)) throw new Error(`Quick Chat UI is missing ${marker}`);
}
if (!quickChat.includes('QuickChatEmojiCatalog.EMOJIS.chunked(6)')) {
  throw new Error('Quick Chat must render a 6-column emoji grid.');
}
if (!quickChat.includes('background = plywoodBackground(')) {
  throw new Error('Quick Chat popup must use the shared plywood background.');
}
const emojiCell = quickChat.match(/val cell = TextView\(context\)\.apply \{([\s\S]*?)\n            \}/);
if (!emojiCell || !emojiCell[1].includes('background = null')) {
  throw new Error('Quick Chat emoji cells must not render circular background chips.');
}
if (quickChat.includes('setColor(0x1CFFFFFF)')) {
  throw new Error('Quick Chat emoji circle chips must remain removed.');
}
for (const marker of ['selectionInProgress', '.scaleX(1.26f)', '.scaleY(1.26f)', 'onEmojiSelected(emoji)']) {
  if (!quickChat.includes(marker)) throw new Error(`Quick Chat emoji selection animation is missing ${marker}`);
}

for (const [label, source] of [
  ['offline profile rail', read('offlineRail')],
  ['online profile rail', read('onlinePresentation')],
]) {
  for (const marker of ['QuickChatButtonView', 'showQuickChatPopup', 'quickChatEnabled']) {
    if (!source.includes(marker)) throw new Error(`${label} is missing ${marker}`);
  }
}
if (!read('reactionPolicy').includes('QuickChatEmojiCatalog.EMOJIS.toSet()')) {
  throw new Error('Offline reactions must accept the complete Quick Chat emoji catalog.');
}
if (!read('offlineRail').includes('senderPlayerId = player.playerId')) {
  throw new Error('Offline Quick Chat must attribute the reaction to the profile whose icon was selected.');
}
for (const marker of [
  'val reserveProfileFooter =',
  'setPadding(\n                    0,\n                    if (reserveProfileFooter) dp(18) else 0',
  'val chatSlot =',
]) {
  if (!read('offlineRail').includes(marker)) {
    throw new Error(`Offline profile/dice alignment is missing ${marker}`);
  }
}
const offlineFeedback = fs.readFileSync('android/app/src/main/java/com/ludoproof/game/feature/offline/presentation/feedback/OfflineLudoPawsFeedback.kt', 'utf8');
if (offlineFeedback.includes('presentComputerQuickChat') || offlineFeedback.includes('presentQuickReaction(')) {
  throw new Error('CPU players must not auto-send Quick Chat until event-frequency policy is explicitly re-enabled.');
}

const settings = read('settingsUi');
for (const label of ['Music', 'Sound', 'Quick chat', 'Game Speed', 'Boards', 'Dice']) {
  if (!settings.includes(`label = "${label}"`)) {
    throw new Error(`Settings UI is missing the required ${label} row.`);
  }
}
for (const removed of ['label = "Animal Voices"', 'label = "Haptics"', 'label = "Game Sounds"']) {
  if (settings.includes(removed)) throw new Error(`Obsolete Settings row is still visible: ${removed}`);
}
if (!settings.includes('settingsPrivacyLink(')) {
  throw new Error('Settings UI must retain its Privacy Policy link.');
}
const settingsKit = read('settingsKit');
const themedPlywoodSurfaces = settingsKit.match(/background\s*=\s*plywoodBackground\(/g) || [];
if (themedPlywoodSurfaces.length < 2) {
  throw new Error('Settings popup and option dialogs must use the shared plywood theme background.');
}
if (!read('offlineRail').includes('isComputerMode') || !read('offlineRail').includes('hasComputerOpponent')) {
  throw new Error('Offline Quick Chat must be reserved for the human in Computer mode, never Pass & Play.');
}
if (!read('onlinePresentation').includes('quickChatEnabled && localPlayer')) {
  throw new Error('Online Quick Chat must be visible only on the local player profile.');
}
if (!read('onlinePresentation').includes('sendQuickChat(emoji)') || !read('onlinePresentation').includes('presentRemoteQuickChat')) {
  throw new Error('Online Quick Chat must send and render real remote reactions.');
}
const realtime = fs.readFileSync('android/app/src/main/java/com/ludoproof/game/feature/online/data/realtime/MatchRealtimeClient.kt', 'utf8');
if (!realtime.includes('QUICK_CHAT_SEND') || !realtime.includes('type == "QUICK_CHAT"')) {
  throw new Error('Realtime client must implement Quick Chat send/receive.');
}
const serverRoom = fs.readFileSync('server/src/match-room.js', 'utf8');
for (const marker of ['QUICK_CHAT_SEND', 'QUICK_CHAT', 'getTags(socket)', 'QUICK_CHAT_COOLDOWN_MS']) {
  if (!serverRoom.includes(marker)) throw new Error(`Server Quick Chat handler is missing ${marker}`);
}
if (!read('settingsEntry').includes('onChanged: (() -> Unit)? = null')) {
  throw new Error('Settings changes must notify the online UI so Quick Chat visibility refreshes.');
}

console.log('Ludo Paws Quick Chat/settings UI gate passed: 24 unique emojis, profile controls on offline/online, compact Settings rows and shared WebP panel background.');
