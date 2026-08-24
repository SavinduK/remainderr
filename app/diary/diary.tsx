import { FontAwesome5, Ionicons } from "@expo/vector-icons";
import { useFocusEffect } from "@react-navigation/native";
import { FlashList } from "@shopify/flash-list";
import { Directory, File, Paths } from "expo-file-system";
import { useRouter } from "expo-router";
import React, { useCallback, useEffect, useRef, useState } from "react";
import {
  Alert,
  Image,
  Modal,
  Pressable,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
  useColorScheme,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import Footer from "../footer";

const ImageGrid = ({ images }: { images: string[] }) => {
  if (!images || images.length === 0) return null;

  const count = images.length;

  if (count === 1) {
    return <Image source={{ uri: images[0] }} style={styles.singleImage} resizeMode="cover" />;
  }

  if (count === 2) {
    return (
      <View style={styles.gridContainer}>
        <Image source={{ uri: images[0] }} style={styles.halfImage} resizeMode="cover" />
        <Image source={{ uri: images[1] }} style={styles.halfImage} resizeMode="cover" />
      </View>
    );
  }

  if (count === 3) {
    return (
      <View style={styles.gridContainer}>
        <View style={styles.gridLeft}>
          <Image source={{ uri: images[0] }} style={styles.fullSize} resizeMode="cover" />
        </View>
        <View style={styles.gridRightColumn}>
          <Image source={{ uri: images[1] }} style={styles.halfHeightImage} resizeMode="cover" />
          <Image source={{ uri: images[2] }} style={styles.halfHeightImage} resizeMode="cover" />
        </View>
      </View>
    );
  }

  if (count === 4) {
    return (
      <View style={styles.gridContainer}>
        <View style={styles.gridRightColumn}>
          <Image source={{ uri: images[0] }} style={styles.halfHeightImage} resizeMode="cover" />
          <Image source={{ uri: images[1] }} style={styles.halfHeightImage} resizeMode="cover" />
        </View>
        <View style={styles.gridRightColumn}>
          <Image source={{ uri: images[2] }} style={styles.halfHeightImage} resizeMode="cover" />
          <Image source={{ uri: images[3] }} style={styles.halfHeightImage} resizeMode="cover" />
        </View>
      </View>
    );
  }

  const extraCount = count - 4;
  return (
    <View style={styles.gridContainer}>
      <View style={styles.gridLeft}>
        <Image source={{ uri: images[0] }} style={styles.fullSize} resizeMode="cover" />
      </View>
      <View style={styles.gridRightWrap}>
        <Image source={{ uri: images[1] }} style={styles.miniImage} resizeMode="cover" />
        <Image source={{ uri: images[2] }} style={styles.miniImage} resizeMode="cover" />
        <Image source={{ uri: images[3] }} style={styles.miniImage} resizeMode="cover" />
        <View style={styles.miniImageContainer}>
          <Image source={{ uri: images[4] }} style={styles.fullSize} resizeMode="cover" />
          {extraCount > 0 && (
            <View style={styles.overlay}>
              <Text style={styles.overlayText}>+{extraCount}</Text>
            </View>
          )}
        </View>
      </View>
    </View>
  );
};

const EntryCard = ({
  item,
  theme,
  onOpenMenu,
}: {
  item: any;
  theme: any;
  onOpenMenu: (targetRef: View, item: any) => void;
}) => {
  const router = useRouter();
  const dotsRef = useRef<React.ElementRef<typeof TouchableOpacity>>(null);

  const entryDate = item.json.date
    ? new Date(item.json.date).toLocaleDateString("en-US", {
      weekday: "short",
      month: "short",
      day: "numeric",
    })
    : "Today";

  return (
    <View style={[styles.card, { backgroundColor: theme.cardBg, borderColor: theme.border }]}>
      <Pressable
        onPress={() =>
          router.push(`./editEntry?path=${encodeURIComponent(item.folder.uri)}`)
        }
      >
        <ImageGrid images={item.json.absoluteImages} />

        <View style={styles.cardContent}>
          {item.json.title ? (
            <Text style={[styles.cardTitle, { color: theme.text }]} numberOfLines={1}>
              {item.json.title}
            </Text>
          ) : null}

          {item.json.text ? (
            <Text style={[styles.cardText, { color: theme.subText }]} numberOfLines={3}>
              {item.json.text}
            </Text>
          ) : null}
        </View>

        {/* Divider above date */}
        <View style={[styles.footerDivider, { backgroundColor: theme.border }]} />

        <View style={styles.cardFooter}>
          <Text style={[styles.dateText, { color: theme.subText }]}>{entryDate}</Text>
          <TouchableOpacity
            ref={dotsRef}
            hitSlop={12}
            onPress={() => dotsRef.current && onOpenMenu(dotsRef.current, item)}
          >
            <Ionicons name="ellipsis-horizontal" size={20} color={theme.subText} />
          </TouchableOpacity>
        </View>
      </Pressable>
    </View>
  );
};

export default function Index() {
  const router = useRouter();
  const colorScheme = useColorScheme();
  const isDark = colorScheme === "dark";

  const [entries, setEntries] = useState<any[]>([]);
  const [selectedActionEntry, setSelectedActionEntry] = useState<any>(null);
  const [menuPosition, setMenuPosition] = useState<{ top: number; right: number } | null>(null);
  const [backupModalVisible, setBackupModalVisible] = useState<boolean>(false);

  const theme = {
    bg: isDark ? "#000000" : "#F2F2F7",
    cardBg: isDark ? "#1C1C1E" : "#FFFFFF",
    text: isDark ? "#FFFFFF" : "#000000",
    subText: isDark ? "#8E8E93" : "#6C6C70",
    border: isDark ? "#2C2C2E" : "#E5E5EA",
    accent: "#0A84FF",
    danger: "#FF453A",
  };

  const loadEntries = useCallback(async () => {
    try {
      const appDir = new Directory(Paths.document, "App");
      if (!appDir.exists) await appDir.create({ intermediates: true });

      const items = appDir.list();
      const entryPromises = items
        .filter((item) => item instanceof Directory)
        .map(async (entryFolder) => {
          if (entryFolder instanceof Directory) {
            const dataFile = new File(entryFolder, "data.json");
            if (!dataFile.exists) return null;

            try {
              const content = await dataFile.text();
              const json = JSON.parse(content);
              const imgsFolder = new Directory(entryFolder, "imgs");

              const absoluteImageUris = (json.images || []).map((filename: string) => {
                const imgFile = new File(imgsFolder, filename);
                return imgFile.uri;
              });

              return {
                folder: entryFolder,
                json: { ...json, absoluteImages: absoluteImageUris },
              };
            } catch (err) {
              console.warn(`Skipping corrupted folder: ${entryFolder.name}`, err);
              return null;
            }
          }
          return null;
        });

      const parsed = await Promise.all(entryPromises);
      const validEntries = parsed
        .filter((item): item is { folder: Directory; json: any } => item !== null)
        .sort((a, b) => b.json.id - a.json.id);

      setEntries(validEntries);
    } catch (e) {
      console.error("Load error:", e);
    }
  }, []);

  useEffect(() => {
    loadEntries();
  }, []);

  useFocusEffect(
    useCallback(() => {
      loadEntries();
    }, [])
  );

  const handleBackupData = async () => {
    try {
      const diaryDir = new Directory(Paths.document, "App");
      if (!diaryDir.exists) {
        Alert.alert("Notice", "No data found to backup.");
        return;
      }
      const backupDir = await Directory.pickDirectoryAsync();
      if (!backupDir) return;
      for (const item of backupDir.list()) {
        await item.delete();
      }
      if (diaryDir.exists) {
        for (const dir of diaryDir.list()) {
          if (dir instanceof Directory) {
            const copyDir = backupDir.createDirectory(dir.name);
            for (const item of dir.list()) {
              console.log(item.name)
              if (item instanceof File) {
                const newFile = copyDir.createFile(item.name, 'application/json');
                const data = await item.bytes();
                await newFile.write(data);
                console.log(newFile.info());
              }
              if (item instanceof Directory) {
                const imgDir = copyDir.createDirectory("imgs");
                for (const img of item.list()) {
                  if (img instanceof File) {
                    console.log(img.name)
                    const newFile = imgDir.createFile(img.name, 'image/jpeg');
                    const data = await img.bytes();
                    await newFile.write(data);
                    console.log(newFile.info());
                  }
                }
              }
            }
          }
        }
      }
      // Alert.alert("Success", "Backup completed successfully!");
    } catch (err) {
      console.error("Backup error:", err);
      //Alert.alert("Error", "Failed to complete backup.");
    }
  };

  const restoreData = async () => {
    try {
      const backupDir = await Directory.pickDirectoryAsync();
      if (!backupDir) return;

      const diaryDir = new Directory(Paths.document, "App");

      if (diaryDir.exists) {
        for (const item of diaryDir.list()) {
          await item.delete();
        }
      } else {
        diaryDir.create();
      }
      for (const subDir of backupDir.list()) {
        if (subDir instanceof Directory && !(subDir.name === "Notes")) {
          const targetDir = new Directory(diaryDir, subDir.name);
          if (!targetDir.exists) targetDir.create();

          for (const item of subDir.list()) {
            if (item instanceof File) {
              const newFile = new File(targetDir, item.name);
              newFile.create();

              const data = await item.bytes();
              await newFile.write(data);
              console.log("Restored file path:", newFile.uri);
            }
            else if (item instanceof Directory) {
              const restoredImgDir = new Directory(targetDir, item.name);
              if (!restoredImgDir.exists) restoredImgDir.create();

              for (const img of item.list()) {
                if (img instanceof File) {
                  const newImg = new File(restoredImgDir, img.name);
                  newImg.create();

                  const data = await img.bytes();
                  await newImg.write(data);
                  console.log("Restored image path:", newImg.uri);
                }
              }
            }
          }
        }
      }

      console.log("App folder contents:", diaryDir.list());
      await loadEntries();
      //Alert.alert("Success", "Data restored successfully!");
    } catch (err) {
      console.error("Restore error:", err);
      // Alert.alert("Error", "Failed to restore data.");
    }
  };

  const deleteEntry = async (folder: Directory) => {
    try {
      await folder.delete();
      setSelectedActionEntry(null);
      setMenuPosition(null);
      loadEntries();
    } catch (e) {
      console.error("Failed to delete entry folder:", e);
    }
  };

  const handleOpenMenu = (targetRef: View, item: any) => {
    targetRef.measureInWindow((x, y, width, height) => {
      setMenuPosition({
        top: y + height + 6,
        right: 16,
      });
      setSelectedActionEntry(item);
    });
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.bg }]}>
      {/* Header */}
      <View style={styles.titleRow}>
        <Text style={[styles.titleText, { color: theme.text }]}>Journal</Text>
        <View style={styles.headerRightBtns}>
          {/* Cloud Icon (Backup/Restore) */}
          <TouchableOpacity
            style={[styles.headerIconBtn, { backgroundColor: theme.cardBg, borderWidth: 1, borderColor: theme.border }]}
            onPress={() => setBackupModalVisible(true)}
          >
            <Ionicons name="cloud-upload-outline" size={20} color={theme.text} />
          </TouchableOpacity>

          {/* Add Icon */}
          <TouchableOpacity
            style={[styles.headerIconBtn, { backgroundColor: theme.accent }]}
            onPress={() => router.push("./addEntry")}
          >
            <FontAwesome5 name="plus" size={16} color="#FFF" />
          </TouchableOpacity>
        </View>
      </View>

      {/* Entry Cards List */}
      <FlashList
        data={entries}
        keyExtractor={(item) => item.folder.name}
        renderItem={({ item }) => (
          <EntryCard item={item} theme={theme} onOpenMenu={handleOpenMenu} />
        )}
      />

      {/* Anchored Card Options Dropdown (3-dots) */}
      {selectedActionEntry && menuPosition && (
        <Modal transparent animationType="none" visible={!!selectedActionEntry}>
          <TouchableOpacity
            style={styles.modalOverlayTransparent}
            activeOpacity={1}
            onPress={() => {
              setSelectedActionEntry(null);
              setMenuPosition(null);
            }}
          >
            <View
              style={[
                styles.dropdownMenu,
                {
                  top: menuPosition.top,
                  right: menuPosition.right,
                  backgroundColor: theme.cardBg,
                  borderColor: theme.border,
                },
              ]}
            >
              <TouchableOpacity
                style={styles.actionItem}
                onPress={() => deleteEntry(selectedActionEntry.folder)}
              >
                <Ionicons name="trash-outline" size={18} color={theme.danger} />
                <Text style={[styles.actionText, { color: theme.danger }]}>Delete Entry</Text>
              </TouchableOpacity>
            </View>
          </TouchableOpacity>
        </Modal>
      )}

      {/* Backup & Restore Modal */}
      <Modal
        transparent
        animationType="fade"
        visible={backupModalVisible}
        onRequestClose={() => setBackupModalVisible(false)}
      >
        <TouchableOpacity
          style={styles.modalOverlay}
          activeOpacity={1}
          onPress={() => setBackupModalVisible(false)}
        >
          <View style={[styles.actionSheet, { backgroundColor: theme.cardBg }]}>
            <Text style={[styles.modalTitle, { color: theme.text }]}>Backup & Restore</Text>

            <TouchableOpacity
              style={styles.actionItem}
              onPress={async () => {
                setBackupModalVisible(false);
                await handleBackupData();
              }}
            >
              <Ionicons name="cloud-upload-outline" size={20} color={theme.accent} />
              <Text style={[styles.actionText, { color: theme.text }]}>Backup Data</Text>
            </TouchableOpacity>

            <View style={[styles.footerDivider, { backgroundColor: theme.border }]} />

            <TouchableOpacity
              style={styles.actionItem}
              onPress={async () => {
                setBackupModalVisible(false);
                await restoreData();
              }}
            >
              <Ionicons name="cloud-download-outline" size={20} color={theme.accent} />
              <Text style={[styles.actionText, { color: theme.text }]}>Restore Data</Text>
            </TouchableOpacity>
          </View>
        </TouchableOpacity>
      </Modal>
      <Footer />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  titleRow: {
    flexDirection: "row",
    paddingHorizontal: 20,
    paddingVertical: 12,
    justifyContent: "space-between",
    alignItems: "center",
  },
  titleText: { fontSize: 28, fontWeight: "800" },
  headerRightBtns: { flexDirection: "row", alignItems: "center", gap: 10 },
  headerIconBtn: { width: 38, height: 38, borderRadius: 19, justifyContent: "center", alignItems: "center" },

  card: {
    width: "92%",
    borderRadius: 20,
    marginVertical: 8,
    marginHorizontal: "4%",
    borderWidth: 0.5,
    overflow: "hidden",
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 8,
    elevation: 2,
  },
  cardContent: { paddingHorizontal: 16, paddingTop: 14, paddingBottom: 12 },
  cardTitle: { fontSize: 18, fontWeight: "700", marginBottom: 6 },
  cardText: { fontSize: 15, lineHeight: 21 },

  /* Footer & Divider Line */
  footerDivider: {
    height: 1,
    width: "100%",
  },
  cardFooter: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  dateText: { fontSize: 13, fontWeight: "600" },

  /* Dynamic Image Grid Styling */
  singleImage: { width: "100%", height: 180 },
  gridContainer: { flexDirection: "row", height: 180, width: "100%", gap: 4, padding: 4 },
  halfImage: { flex: 1, height: "100%", borderRadius: 8 },
  gridLeft: { flex: 1, height: "100%" },
  gridRightColumn: { flex: 1, flexDirection: "column", gap: 4 },
  gridRightWrap: { flex: 1, flexDirection: "row", flexWrap: "wrap", gap: 4 },
  halfHeightImage: { flex: 1, width: "100%", borderRadius: 8 },
  miniImage: { width: "48%", height: "48%", borderRadius: 8 },
  miniImageContainer: { width: "48%", height: "48%", borderRadius: 8, overflow: "hidden", position: "relative" },
  fullSize: { width: "100%", height: "100%", borderRadius: 8 },
  overlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "rgba(0,0,0,0.45)",
    justifyContent: "center",
    alignItems: "center",
  },
  overlayText: { color: "#FFF", fontSize: 16, fontWeight: "700" },

  /* Anchored Dropdown & Backup Modal */
  modalOverlayTransparent: { flex: 1, backgroundColor: "transparent" },
  modalOverlay: { flex: 1, backgroundColor: "rgba(0,0,0,0.4)", justifyContent: "flex-end" },
  dropdownMenu: {
    position: "absolute",
    borderRadius: 12,
    borderWidth: 0.5,
    paddingVertical: 4,
    paddingHorizontal: 8,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.15,
    shadowRadius: 10,
    elevation: 8,
    minWidth: 140,
  },
  actionSheet: { borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 20, gap: 4 },
  modalTitle: { fontSize: 18, fontWeight: "700", marginBottom: 12, textAlign: "center" },
  actionItem: { flexDirection: "row", alignItems: "center", gap: 12, paddingVertical: 14, paddingHorizontal: 8 },
  actionText: { fontSize: 16, fontWeight: "600" },
});