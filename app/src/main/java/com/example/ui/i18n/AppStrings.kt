package com.example.ui.i18n

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf

enum class AppLanguage(val code: String, val displayName: String) {
    SW("sw", "Kiswahili"),
    EN("en", "English")
}

/**
 * Interface-based localization table (0 constructor parameters) to keep DEX/ART bytecode
 * compact and completely immune to the 255-register constructor argument limit.
 */
@Immutable
interface AppStrings {
    val language: AppLanguage

    // Bottom bar (2 tabs only)
    val tabMessages: String
    val tabContacts: String

    // Top bar & badges
    val badgeOffline: String
    val cdOpenDrawer: String
    val cdSettings: String
    val cdLockApp: String
    val cdToggleTheme: String
    val cdBack: String

    // Slide bar / Drawer sections
    val drawerSubtitleOffline: String
    val drawerAesActive: String
    val drawerSimAuto: String
    val drawerSectionTools: String
    val drawerSectionVault: String
    val drawerSectionPrefs: String
    val drawerSecurityLevel: String
    val drawerCompose: String
    val drawerComposeSub: String
    val drawerSchedule: String
    val drawerScheduleSub: String
    val drawerTemplates: String
    val drawerTemplatesSub: String
    val drawerSecurityCenter: String
    val drawerSecurityCenterSub: String
    val drawerHiddenLogs: String
    val drawerHiddenLogsSub: String
    val drawerTrash: String
    val drawerTrashSub: String
    val drawerSettings: String
    val drawerSettingsSub: String
    val drawerUpgrade: String
    val drawerUpgradeSub: String
    val drawerUpgradeAvailableBadge: String
    val upgradeScreenTitle: String
    val upgradeScreenSubtitle: String
    val drawerDarkMode: String
    val drawerDarkModeSub: String
    val drawerLanguage: String
    val drawerLockNow: String
    val drawerSetPasscodeFirst: String

    // Inbox & Reports
    val statDelivered: String
    val statSent: String
    val statSilentUnknown: String
    val filterAll: String
    val filterUnknownSilent: String
    val filterReports: String
    val filterSecret: String
    val inboxEmptyAll: String
    val inboxEmptyUnknown: String
    val inboxEmptyReports: String
    val inboxEmptySecret: String
    val inboxEmptyHint: String
    val btnNewMessage: String
    val statusDelivered: String
    val statusSent: String
    val statusSending: String
    val statusFailed: String
    val statusSilentUnknown: String
    val statusReceived: String
    val secretCardMask: String
    val reportPrefix: String
    val partsWord: String
    val secretReaderAutoBurnDesc: String
    val secretReaderNormalDesc: String
    val secretReaderBurnBtn: String
    val secretReaderCloseOnlyBtn: String

    // Contacts screen (2nd bottom tab)
    val contactsTitle: String
    val contactsSearchHint: String
    val contactsAddFriend: String
    val contactsSelectAll: String
    val contactsClearAll: String
    val contactsSelectedCount: (Int) -> String
    val contactsSendToSelected: (Int) -> String
    val contactsEmptyTitle: String
    val contactsEmptyHint: String
    val contactsGrantPerm: String
    val contactsQuickSms: String

    // Compose screen
    val composeTitle: String
    val permBannerTitle: String
    val permBannerSub: String
    val btnGrant: String
    val simSelectorHeader: String
    val simDefaultFallback: String
    val recipientsHeader: (Int) -> String
    val btnAddNumber: String
    val btnHideList: String
    val btnShowList: String
    val pickManyHint: String
    val btnAll: String
    val btnClear: String
    val quickTemplatesHeader: String
    val unlimitedHeader: String
    val charPartsCounter: (Int, Int) -> String
    val unlimitedPlaceholder: String
    val secretBurnToggleTitle: String
    val secretBurnToggleSub: String
    val btnSchedule: String
    val btnSendCount: (Int) -> String
    val btnSystemSmsFallback: String

    // Add Recipient Dialog
    val addFriendDialogTitle: String
    val friendNameLabel: String
    val friendPhoneLabel: String
    val saveFriendCheckbox: String
    val btnAdd: String
    val btnCancel: String

    // Schedule Dialog & Screen
    val scheduleTitle: String
    val scheduleDialogTitle: String
    val scheduleWillSendAt: (String) -> String
    val btnSaveSchedule: String
    val scheduleEmptyTitle: String
    val scheduleEmptyHint: String
    val btnWriteAndSchedule: String
    val schedulePendingHeader: (Int) -> String
    val btnNewShort: String
    val btnSendNow: String
    val schedulePresets: List<Pair<Int, String>>

    // Templates Screen
    val templatesTitle: String
    val templatesSearchHint: String
    val btnUseTemplate: String
    val btnNewTemplate: String
    val dialogCreateTemplate: String
    val dialogEditTemplate: String
    val templateTitleLabel: String
    val templateCategoryLabel: String
    val templateBodyLabel: (Int) -> String
    val btnSaveAes: String
    val charsWord: String

    // Security Center
    val securityTitle: String
    val stealthCardTitle: String
    val stealthCardSub: String
    val toggleSilentSmsTitle: String
    val toggleSilentSmsSub: String
    val toggleBlockCallsTitle: String
    val toggleBlockCallsSub: String
    val callRoleActiveTitle: String
    val callRoleInactiveTitle: String
    val callRoleActiveSub: String
    val callRoleInactiveSub: String
    val btnEnable: String
    val toggleAutoBurnTitle: String
    val toggleAutoBurnSub: String
    val simCardTitle: String
    val simNoneFound: String
    val simActiveBadge: String
    val lockCardTitle: String
    val lockCardPassSet: String
    val lockCardNoPass: String
    val btnChangePasscode: String
    val btnSetPasscode: String
    val btnRemove: String
    val idleLockHeader: String
    val toggleLockScreenOffTitle: String
    val toggleLockScreenOffSub: String
    val vaultSummaryTitle: String
    val vaultSummarySub: (Int) -> String
    val btnHiddenFolderShort: String
    val btnTrashCount: (Int) -> String
    val iconVisibleLabel: String
    val iconDisguiseLabel: String
    val iconHiddenLabel: String
    val passcodeDialogTitle: String
    val passcodeDialogDesc: String
    val passcodeNewLabel: String
    val passcodeConfirmLabel: String
    val passcodeMismatch: String
    val btnSavePasscode: String

    // Settings Screen
    val settingsTitle: String
    val settingsLanguageCard: String
    val settingsLanguageSub: String
    val settingsNotifCard: String
    val settingsNotifSub: String
    val settingsBubbleTitle: String
    val settingsBubbleSub: String
    val settingsOpenSystemBubbles: String
    val settingsAppearanceCard: String
    val settingsAppearanceSub: String
    val settingsDarkModeTitle: String
    val settingsDarkModeSub: String
    val settingsDimLightTitle: String
    val settingsDimLightSub: String
    val settingsDimLightLevel: (Int) -> String
    val settingsStealthIconCard: String
    val settingsStealthIconSub: String
    val settingsSimPermsCard: String
    val settingsSimPermsSub: String
    val settingsRefreshSim: String
    val settingsManagePerms: String
    val settingsQuickSecurity: String
    val settingsAboutOffline: String

    // Trash Screen
    val trashTitle: String
    val trashSub: (Int) -> String
    val btnEmptyAll: String
    val trashEmptyTitle: String
    val trashEmptyHint: String
    val trashDeletedMsgs: (Int) -> String
    val trashDeletedTmpls: (Int) -> String
    val btnRestore: String
    val btnDelete: String

    // Hidden Logs Screen
    val hiddenLogsTitle: String
    val keystoreHwReady: String
    val keystoreSwReady: String
    val hiddenFolderExplain: String
    val hiddenStatsLine: (Int, Long) -> String
    val btnEncryptFile: String
    val encryptedFilesHeader: (Int) -> String
    val activityLogsHeader: (Int) -> String
    val btnClearLogs: String
    val logsEmptyHint: String
    val encryptDialogTitle: String
    val encryptFileNameLabel: String
    val encryptFileBodyLabel: String
    val btnEncryptAndSave: String
    val btnClose: String

    // Lock Screen & Icon Dialog
    val lockScreenTitle: String
    val lockScreenDesc: String
    val lockScreenPassLabel: String
    val lockScreenUnlockBtn: String
    val iconDialogTitle: String
    val iconDialogDesc: String
    val iconOptVisibleTitle: String
    val iconOptVisibleSub: String
    val iconOptCalcTitle: String
    val iconOptCalcSub: String
    val iconOptHiddenTitle: String
    val iconOptHiddenSub: String

    // Security levels
    val levelTitle: (Int) -> String
    val levelDesc: (Int) -> String

    // Permissions Hub (Contacts, Storage, Mic, SMS) & Voice Dictation
    val permHubTitle: String
    val permHubSub: String
    val permSectionDrawer: String
    val permStatusGranted: String
    val permStatusNeeded: String
    val permContactsLabel: String
    val permContactsDesc: String
    val permStorageLabel: String
    val permStorageDesc: String
    val permStorageGrantedDesc: (String) -> String
    val permMicLabel: String
    val permMicDesc: String
    val permSmsLabel: String
    val permSmsDesc: String
    val permGrantAllBtn: String
    val voiceDictateBtn: String
    val voiceListeningHint: String
    val voiceStopBtn: String
    val vaultImportFileBtn: String
    val vaultImportPhotoBtn: String

    // Backup & Restore Center (All Methods)
    val drawerBackupRestore: String
    val drawerBackupRestoreSub: String
    val backupCenterTitle: String
    val backupCenterHeroTitle: String
    val backupCenterHeroSub: String
    val backupTabCloud: String
    val backupTabDevice: String
    val backupCloudTitle: String
    val backupCloudSub: String
    val backupSignInGoogleBtn: String
    val backupSignInDesc: String
    val backupSignOutBtn: String
    val backupSignedInAs: String
    val backupNowCloudBtn: String
    val backupCloudEmpty: String
    val backupCloudLoading: String
    val backupCloudError: String
    val backupRenameTitle: String
    val backupRenameLabel: String
    val backupFileTitle: String
    val backupFileSub: String
    val backupExportFileBtn: String
    val backupImportFileBtn: String
    val backupShareTitle: String
    val backupShareSub: String
    val backupShareBtn: String
    val backupLocalTitle: String
    val backupLocalSub: String
    val backupCreateSnapshotBtn: String
    val backupLocalEmpty: String
    val backupSystemSmsTitle: String
    val backupSystemSmsSub: String
    val backupImportSystemSmsBtn: String
    val backupTrashTitle: String
    val backupTrashSub: (Int) -> String
    val backupOpenTrashBtn: String
    val backupDialogCreateTitle: String
    val backupDialogLabelField: String
    val backupDialogPassField: String
    val backupDialogPassHint: String
    val backupDialogRestoreTitle: String
    val backupDialogRestorePassLabel: String
    val backupRestoreModeMerge: String
    val backupRestoreModeMergeSub: String
    val backupRestoreModeReplace: String
    val backupRestoreModeReplaceSub: String
    val backupBtnConfirmBackup: String
    val backupBtnConfirmRestore: String
    val backupBadgePassProtected: String
    val backupBadgeAesAuto: String
    val backupItemsSummary: (Int, Int, Int) -> String

    // Premium Conversation Chat UI
    val convSearchHint: String
    val convToday: String
    val convYesterday: String
    val convYouPrefix: String
    val convInputPlaceholder: String
    val convSecretArmed: String
    val convMessageReportTitle: String
    val convCopyText: String
    val convMoveToTrash: String
    val convNoMessagesYet: String
    val convStartTypingHint: String
    val settingsBackupCardTitle: String
    val settingsBackupCardSub: String
    val settingsBackupOpenBtn: String

    // Ultra-Strong Fortress Security
    val fortressScoreTitle: String
    val fortressMaxBtn: String
    val fortressCardTitle: String
    val fortressCardSub: String
    val toggleBlockScreenshotsTitle: String
    val toggleBlockScreenshotsSub: String
    val togglePrivacyCurtainTitle: String
    val togglePrivacyCurtainSub: String
    val toggleClipboardClearTitle: String
    val toggleClipboardClearSub: String
    val toggleIntruderWipeTitle: String
    val toggleIntruderWipeSub: String
    val privacyCurtainTitle: String
    val privacyCurtainSub: String
    val lockCooldownBanner: (Int) -> String
    val lockAttemptsWarning: (Int, Int) -> String
    val convToolSecret: String
    val convToolTemplates: String
    val convToolVoice: String
    val convEncryptedChip: String

    // Contact Profile & Long-Press Message Actions (Copy, Share, Forward)
    val convTapProfileHint: String
    val convActionCopy: String
    val convActionShare: String
    val convActionForward: String
    val convActionInfo: String
    val convActionTrash: String
    val convProfileTitle: String
    val convProfileCall: String
    val convProfileSearch: String
    val convProfileSchedule: String
    val convProfileCopyNumber: String
    val convProfileStatsMessages: String
    val convProfileStatsSecret: String
    val convProfileEncryptionTitle: String
    val convProfileEncryptionSub: String
    val convProfileSaveContact: String
    val convProfileNumberCopied: String
    val convForwardTitle: String
    val convForwardSearchHint: String
    val convForwardSendBtn: (Int) -> String
    val convForwardManualAdd: String
    val convSearchInChatHint: String
    val convEncryptedTimelineNotice: String

    // Hold Multi-Select (Mark All / Unmark All / Delete All) & Pop Reply
    val convMarkAll: String
    val convUnmarkAll: String
    val convDeleteAll: String
    val convSelectedCount: (Int) -> String
    val convDeleteSelectedTitle: String
    val convDeleteSelectedBody: (Int) -> String
    val popBadgeLabel: String
    val popReplyBtn: String
    val popReplyPlaceholder: String
    val popReplySend: String
    val popOpenChat: String

    // Smart Search, Conversation Attachments (+) & Text Delivery Reports
    val searchScopeAll: String
    val searchScopeNames: String
    val searchScopeMessages: String
    val searchScopeNumbers: String
    val searchDetectedNumber: String
    val searchDetectedContact: String
    val searchDetectedMessage: String
    val searchMatchesCount: (Int) -> String
    val searchStartNewChat: String
    val attachSheetTitle: String
    val attachPhoto: String
    val attachDocument: String
    val attachContactNumber: String
    val attachTemplate: String
    val attachSecretSms: String
    val attachScheduleSms: String
    val attachPickContactTitle: String
    val attachReadyLabel: String
    val convToolDeliveryReport: String
    val settingsDeliveryReportTitle: String
    val settingsDeliveryReportSub: String
    val deliveryLabelSending: String
    val deliveryLabelSent: String
    val deliveryLabelDelivered: String
    val deliveryLabelFailed: String
    val defaultSmsBannerTitle: String
    val defaultSmsBannerSub: String
    val defaultSmsSetNowBtn: String
    val defaultSmsActiveBadge: String
    val settingsOwnershipCardTitle: String
    val settingsOwnershipCardSub: String
    val settingsDefaultSmsRowTitle: String
    val settingsDefaultSmsRowSub: String
    val settingsCallShieldRowTitle: String
    val settingsCallShieldRowSub: String
    val settingsSyncPhoneSmsBtn: String
    val settingsTakeFullControlBtn: String
    val settingsRoleActive: String
    val settingsRoleActivate: String
    val drawerUsageDashboard: String
    val drawerUsageDashboardSub: String
    val dashboardTitle: String
    val dashboardSubtitle: String
    val dashboardRange7d: String
    val dashboardRange14d: String
    val dashboardRange30d: String
    val dashboardKpiSent: String
    val dashboardKpiReceived: String
    val dashboardKpiDeliveryRate: String
    val dashboardKpiDataSaved: String
    val dashboardTotalAll: String
    val dashboardBlockedShort: String
    val dashboardConfirmedShort: String
    val dashboardZeroDataBadge: String
    val dashboardDailyChartTitle: String
    val dashboardDailyChartSub: (Int) -> String
    val dashboardChartArea: String
    val dashboardChartBar: String
    val dashboardTopContactsTitle: String
    val dashboardTopContactsSub: String
    val dashboardNoDataYet: String
    val dashboardSavingsTitle: String
    val dashboardSavingsSub: String
    val dashboardSavingsMobileData: String
    val dashboardSavingsStorage: String
    val dashboardSavingsStorageSub: String
    val dashboardSavingsVaultUsed: String
    val dashboardSavedShort: String
    val dashboardSecurityImpactTitle: String
    val dashboardSecurityImpactSub: String
    val dashboardImpactSilentBlocked: String
    val dashboardImpactSecretBurn: String
    val dashboardImpactScheduled: String
    val dashboardImpactDelivered: String
    val contactDetailsTitle: String
    val contactDetailsPhones: String
    val contactDetailsEmails: String
    val contactDetailsWork: String
    val contactDetailsAddress: String
    val contactDetailsNotes: String
    val contactDetailsSourceSystem: String
    val contactDetailsSourceLocal: String
    val contactDetailsSourceUnsaved: String
    val contactDetailsSourceService: String
    val contactDetailsOpenChat: String
    val contactDetailsShareContact: String
    val contactDetailsOpenSystemContact: String
    val contactDetailsActivityTitle: String
    val contactDetailsLastActivity: String
    val contactDetailsNoMessagesYet: String
    val contactDetailsViewProfileCd: String
    val settingsMinimizedTapHint: String
    val settingsAllGrantedSummary: (Int, Int) -> String
    val settingsActiveShort: String
    val settingsOffShort: String
    val settingsModeDarkShort: String
    val settingsModeLightShort: String
    val launchSplashTagline: String
    val filterBlockedAds: String
    val inboxEmptyBlockedAds: String
    val adBlockerCardTitle: String
    val adBlockerCardSub: String
    val adBlockerToggleTitle: String
    val adBlockerToggleSub: String
    val adBlockerSweepBtn: String
    val adBlockerDeleteAllBtn: String
    val adBlockerAllowSenderBtn: String
    val adBlockerProtectedChip: String
    val adBlockerBannerTitle: (Int) -> String
    val adBlockerBannerSub: String

    // Full Notification Manager
    val drawerNotifManager: String
    val drawerNotifManagerSub: String
    val settingsOpenNotifManagerBtn: String
    val notifMgrTitle: String
    val notifMgrSubtitle: String
    val notifMgrMasterTitle: String
    val notifMgrMasterSub: String
    val notifMgrPermissionOk: String
    val notifMgrPermissionMissing: String
    val notifMgrGrantPermBtn: String
    val notifMgrActiveCount: (Int) -> String
    val notifMgrTestBtn: String
    val notifMgrClearAllBtn: String
    val notifMgrSectionCategories: String
    val notifMgrSectionCategoriesSub: String
    val notifMgrCatPersonal: String
    val notifMgrCatPersonalSub: String
    val notifMgrCatTxn: String
    val notifMgrCatTxnSub: String
    val notifMgrCatBubbles: String
    val notifMgrCatBubblesSub: String
    val notifMgrCatInAppPop: String
    val notifMgrCatInAppPopSub: String
    val notifMgrCatDelivery: String
    val notifMgrCatDeliverySub: String
    val notifMgrCatSilentUnknown: String
    val notifMgrCatSilentUnknownSub: String
    val notifMgrSectionSoundStyle: String
    val notifMgrSectionSoundStyleSub: String
    val notifMgrSoundTitle: String
    val notifMgrSoundSub: String
    val notifMgrRingtoneLabel: String
    val notifMgrRingtoneDefault: String
    val notifMgrRingtonePickBtn: String
    val notifMgrVibrateTitle: String
    val notifMgrVibOff: String
    val notifMgrVibShort: String
    val notifMgrVibDouble: String
    val notifMgrVibHeartbeat: String
    val notifMgrVibLong: String
    val notifMgrHeadsUpTitle: String
    val notifMgrHeadsUpSub: String
    val notifMgrLedTitle: String
    val notifMgrLedSub: String
    val notifMgrBadgeTitle: String
    val notifMgrBadgeSub: String
    val notifMgrSectionPrivacy: String
    val notifMgrSectionPrivacySub: String
    val notifMgrPreviewAll: String
    val notifMgrPreviewAllSub: String
    val notifMgrPreviewSender: String
    val notifMgrPreviewSenderSub: String
    val notifMgrPreviewHide: String
    val notifMgrPreviewHideSub: String
    val notifMgrLivePreviewLabel: String
    val notifMgrSectionActions: String
    val notifMgrSectionActionsSub: String
    val notifMgrActionPopReply: String
    val notifMgrActionPopReplySub: String
    val notifMgrActionMarkRead: String
    val notifMgrActionMarkReadSub: String
    val notifMgrActionMute: String
    val notifMgrActionMuteSub: String
    val notifMgrSectionQuiet: String
    val notifMgrSectionQuietSub: String
    val notifMgrQuietEnable: String
    val notifMgrQuietEnableSub: String
    val notifMgrQuietActiveNow: String
    val notifMgrQuietStart: String
    val notifMgrQuietEnd: String
    val notifMgrQuietAllowTxn: String
    val notifMgrQuietAllowTxnSub: String
    val notifMgrSectionMuted: String
    val notifMgrSectionMutedSub: String
    val notifMgrMuteInputHint: String
    val notifMgrMuteAddBtn: String
    val notifMgrMuteFromChats: String
    val notifMgrMuteEmpty: String
    val notifMgrUnmuteBtn: String
    val notifMgrUnmuteAllBtn: String
    val notifMgrSectionSystem: String
    val notifMgrSectionSystemSub: String
    val notifMgrOpenAppNotifs: String
    val notifMgrOpenBubbles: String
    val notifMgrChannelPersonal: String
    val notifMgrChannelTxn: String
    val notifMgrChannelSecret: String
    val notifMgrChannelDelivery: String
}

fun stringsFor(language: AppLanguage): AppStrings = when (language) {
    AppLanguage.SW -> SwahiliStrings
    AppLanguage.EN -> EnglishStrings
}

val LocalAppStrings = compositionLocalOf<AppStrings> { SwahiliStrings }
