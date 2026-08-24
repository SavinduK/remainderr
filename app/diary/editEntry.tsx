import React, { useEffect, useState } from "react";
import {
  Alert,
  Dimensions,
  Image,
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
  useColorScheme,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { KeyboardAwareScrollView } from "react-native-keyboard-aware-scroll-view";
import { Ionicons } from "@expo/vector-icons";
import * as ImagePicker from "expo-image-picker";
import { Directory, File, Paths } from "expo-file-system";
import { useLocalSearchParams, useRouter } from "expo-router";
import DateTimePicker from "@react-native-community/datetimepicker";

const { width: SCREEN_WIDTH } = Dimensions.get("window");
const HEADER_HEIGHT = 320;

export default function EntryDetail() {
  const router = useRouter();
  const colorScheme = useColorScheme();
  const isDark = colorScheme === "dark";

  const { path } = useLocalSearchParams<{ path: string }>();

  // State
  const [isEditing, setIsEditing] = useState(false);
  const [title, setTitle] = useState("");
  const [text, setText] = useState("");
  const [images, setImages] = useState<string[]>([]);
  const [date, setDate] = useState<Date>(new Date());
  const [id, setId] = useState<number | string>("");
  const [showDatePicker, setShowDatePicker] = useState(false);

  // Dynamic Theme Colors
  const theme = {
    bg: isDark ? "#000000" : "#F8F9FA",
    sheetBg: isDark ? "#1C1C1E" : "#FFFFFF",
    text: isDark ? "#FFFFFF" : "#1A1A1A",
    subText: isDark ? "#8E8E93" : "#71717A",
    inputBg: isDark ? "#2C2C2E" : "#F4F4F5",
    accent: "#0A84FF", 
    danger: "#EF4444",
    border: isDark ? "#3A3A3C" : "#E4E4E7",
  };

  useEffect(() => {
    const setup = async () => {
      if (!path) return;
      try {
        let jsonContent = "";
        let folderPath = path;

        // Check if path points to a Directory or a direct JSON File
        const targetDir = new Directory(path);
        if (targetDir.exists) {
          const dataFile = new File(targetDir, "data.json");
          if (dataFile.exists) {
            jsonContent = await dataFile.text();
            const imgsDir = new Directory(targetDir, "imgs");

            const parsed = JSON.parse(jsonContent);
            setTitle(parsed.title || "");
            setText(parsed.text || "");
            setDate(parsed.date ? new Date(parsed.date) : new Date());
            setId(parsed.id || Date.now());

            // Resolve relative images in folder to full file URIs
            if (parsed.images && Array.isArray(parsed.images)) {
              const fullUris = parsed.images.map((imgName: string) => {
                if (imgName.startsWith("file:") || imgName.startsWith("data:")) {
                  return imgName;
                }
                return new File(imgsDir, imgName).uri;
              });
              setImages(fullUris);
            } else if (parsed.image) {
              setImages([parsed.image]);
            }
            return;
          }
        }

        // Fallback for single file JSON path
        const jsonFile = new File(path);
        if (jsonFile.exists) {
          jsonContent = await jsonFile.text();
          const parsed = JSON.parse(jsonContent);
          setTitle(parsed.title || "");
          setText(parsed.text || "");
          setDate(parsed.date ? new Date(parsed.date) : new Date());
          setId(parsed.id || Date.now());
          setImages(parsed.images || (parsed.image ? [parsed.image] : []));
        }
      } catch (e) {
        console.error("Error loading entry data:", e);
      }
    };
    setup();
  }, [path]);

  // Image Picker Logic
  const pickImages = async () => {
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      quality: 0.8,
      allowsMultipleSelection: true,
    });

    if (!result.canceled && result.assets?.length > 0) {
      const newUris = result.assets.map((asset) => asset.uri);
      setImages((prev) => [...prev, ...newUris]);
    }
  };

  const removeImage = (indexToRemove: number) => {
    setImages((prev) => prev.filter((_, idx) => idx !== indexToRemove));
  };

  // Save / Update Changes
  const saveEntry = async () => {
    if (images.length === 0 && text.trim() === "" && title.trim() === "") {
      Alert.alert("Empty Entry", "Please add at least a title, note, or photo.");
      return;
    }

    try {
      if (!path) {
        router.back();
        return;
      }

      const targetDir = new Directory(path);
      if (targetDir.exists) {
        // Save using folder structure
        const imgsDir = new Directory(targetDir, "imgs");
        if (!imgsDir.exists) await imgsDir.create({ intermediates: true });

        const savedImageFilenames: string[] = [];
        for (let i = 0; i < images.length; i++) {
          const uri = images[i];
          if (uri.includes(targetDir.uri)) {
            // Existing image file in directory
            const filename = uri.split("/").pop() || `img_${i}.jpg`;
            savedImageFilenames.push(filename);
          } else {
            // New local image URI, copy over
            const sourceFile = new File(uri);
            const filename = `img_${i}_${Date.now()}.jpg`;
            const destFile = new File(imgsDir, filename);
            await sourceFile.copy(destFile);
            savedImageFilenames.push(filename);
          }
        }

        const data = {
          id: id || Date.now(),
          title,
          text,
          date: date.toISOString(),
          images: savedImageFilenames,
        };

        const jsonFile = new File(targetDir, "data.json");
        await jsonFile.write(JSON.stringify(data, null, 2));
      } else {
        // Single JSON file write fallback
        const jsonFile = new File(path);
        const data = {
          id: id || Date.now(),
          title,
          text,
          date: date.toISOString(),
          images,
        };
        await jsonFile.write(JSON.stringify(data, null, 2));
      }

      setIsEditing(false);
    } catch (e) {
      console.error("Failed to save file:", e);
      Alert.alert("Save Error", "Unable to save changes.");
    }
  };

  // Delete Entry
  const deleteEntry = async () => {
    Alert.alert("Delete Entry", "Are you sure you want to delete this diary entry?", [
      { text: "Cancel", style: "cancel" },
      {
        text: "Delete",
        style: "destructive",
        onPress: async () => {
          try {
            if (path) {
              const targetDir = new Directory(path);
              if (targetDir.exists) {
                await targetDir.delete();
              } else {
                const targetFile = new File(path);
                if (targetFile.exists) await targetFile.delete();
              }
            }
            router.push("/");
          } catch (e) {
            console.error("Delete failed:", e);
          }
        },
      },
    ]);
  };

  const formatDateLabel = (d: Date) => {
    return d.toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  };

  return (
    <View style={[styles.container, { backgroundColor: theme.bg }]}>
      <KeyboardAwareScrollView
        contentContainerStyle={{ flexGrow: 1 }}
        enableOnAndroid={true}
        bounces={false}
      >
        {/* TOP HEADER / MEDIA CAROUSEL */}
        <View style={styles.headerMediaContainer}>
          {images.length > 0 ? (
            <ScrollView
              horizontal
              pagingEnabled
              showsHorizontalScrollIndicator={false}
              style={styles.headerScrollView}
            >
              {images.map((imgUri, index) => (
                <View key={index} style={styles.imageSlide}>
                  <Image source={{ uri: imgUri }} style={styles.headerImage} resizeMode="cover" />
                  {isEditing && (
                    <TouchableOpacity
                      style={styles.removeImageBadge}
                      onPress={() => removeImage(index)}
                    >
                      <Ionicons name="close" size={16} color="#FFF" />
                    </TouchableOpacity>
                  )}
                </View>
              ))}
            </ScrollView>
          ) : (
            <TouchableOpacity
              activeOpacity={0.8}
              style={[styles.placeholderHeader, { backgroundColor: theme.inputBg }]}
              onPress={pickImages}
            >
              <Ionicons name="image-outline" size={48} color={theme.subText} />
              <Text style={[styles.placeholderText, { color: theme.subText }]}>
                Tap to Add Photos
              </Text>
            </TouchableOpacity>
          )}

          {/* Floating Top Nav Buttons */}
          <SafeAreaView style={styles.topNavOverlay}>
            <TouchableOpacity style={styles.circleBtn} onPress={() => router.back()}>
              <Ionicons name="arrow-back" size={20} color="#1A1A1A" />
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.circleBtn}
              onPress={() => {
                if (isEditing) {
                  saveEntry();
                } else {
                  setIsEditing(true);
                }
              }}
            >
              <Ionicons
                name={isEditing ? "checkmark" : "create-outline"}
                size={20}
                color={isEditing ? theme.accent : "#1A1A1A"}
              />
            </TouchableOpacity>
          </SafeAreaView>

          {/* Floating Action Buttons over Header (Edit Mode) */}
          {isEditing && (
            <View style={styles.floatingActionRow}>
              <TouchableOpacity style={styles.deletePillBtn} onPress={deleteEntry}>
                <Ionicons name="trash" size={16} color="#FFF" />
                <Text style={styles.pillBtnText}>Delete</Text>
              </TouchableOpacity>

              <TouchableOpacity style={styles.addPhotosPillBtn} onPress={pickImages}>
                <Ionicons name="camera" size={16} color="#FFF" />
                <Text style={styles.pillBtnText}>Add Photos</Text>
              </TouchableOpacity>
            </View>
          )}
        </View>

        {/* BOTTOM SHEET CONTENT AREA */}
        <View style={[styles.contentSheet, { backgroundColor: theme.sheetBg }]}>
          {isEditing ? (
            /* EDIT MODE INPUT FORM */
            <View style={styles.formContainer}>
              <Text style={[styles.inputLabel, { color: theme.subText }]}>TITLE</Text>
              <View style={[styles.inputCard, { backgroundColor: theme.inputBg }]}>
                <TextInput
                  value={title}
                  onChangeText={setTitle}
                  placeholder="Enter Title..."
                  placeholderTextColor={theme.subText}
                  style={[styles.textInput, { color: theme.text }]}
                />
              </View>

              <Text style={[styles.inputLabel, { color: theme.subText }]}>DATE</Text>
              <TouchableOpacity
                style={[styles.inputCard, { backgroundColor: theme.inputBg }]}
                onPress={() => setShowDatePicker(true)}
              >
                <Text style={[styles.textInput, { color: theme.text }]}>
                  {formatDateLabel(date)}
                </Text>
              </TouchableOpacity>

              <Text style={[styles.inputLabel, { color: theme.subText }]}>
                MEMORIES & DESCRIPTION
              </Text>
              <View style={[styles.inputCard, styles.noteInputCard, { backgroundColor: theme.inputBg }]}>
                <TextInput
                  multiline
                  value={text}
                  onChangeText={setText}
                  placeholder="No details provided."
                  placeholderTextColor={theme.subText}
                  style={[styles.textInput, styles.noteTextInput, { color: theme.text }]}
                />
              </View>

              <TouchableOpacity style={[styles.saveChangesBtn, { backgroundColor: theme.accent }]} onPress={saveEntry}>
                <Text style={styles.saveChangesText}>Save Changes</Text>
              </TouchableOpacity>
            </View>
          ) : (
            /* VIEW MODE DISPLAY */
            <View style={styles.viewContainer}>
              <View style={styles.dateHeaderRow}>
                <Text style={[styles.dateText, { color: theme.subText }]}>
                  {formatDateLabel(date)}
                </Text>
              </View>

              <Text style={[styles.viewTitle, { color: theme.text }]}>
                {title.trim() ? title : "Untitled Entry"}
              </Text>

              <View style={[styles.divider, { backgroundColor: theme.border }]} />

              <Text style={[styles.viewBodyText, { color: text ? theme.text : theme.subText }]}>
                {text.trim() ? text : "No details provided."}
              </Text>
            </View>
          )}
        </View>
      </KeyboardAwareScrollView>

      {/* Date Picker Modal */}
      {showDatePicker && (
        <Modal transparent animationType="fade" visible={showDatePicker}>
          <TouchableOpacity
            style={styles.modalOverlay}
            activeOpacity={1}
            onPress={() => setShowDatePicker(false)}
          >
            <View style={[styles.modalContent, { backgroundColor: theme.sheetBg }]}>
              <DateTimePicker
                value={date}
                mode="date"
                display="inline"
                onChange={(event, selectedDate) => {
                  if (selectedDate) setDate(selectedDate);
                  setShowDatePicker(false);
                }}
              />
            </View>
          </TouchableOpacity>
        </Modal>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  headerMediaContainer: {
    width: SCREEN_WIDTH,
    height: HEADER_HEIGHT,
    position: "relative",
  },
  headerScrollView: {
    width: SCREEN_WIDTH,
    height: HEADER_HEIGHT,
  },
  imageSlide: {
    width: SCREEN_WIDTH,
    height: HEADER_HEIGHT,
    position: "relative",
  },
  headerImage: {
    width: "100%",
    height: "100%",
  },
  placeholderHeader: {
    width: SCREEN_WIDTH,
    height: HEADER_HEIGHT,
    justifyContent: "center",
    alignItems: "center",
  },
  placeholderText: {
    marginTop: 8,
    fontSize: 14,
    fontWeight: "600",
  },
  removeImageBadge: {
    position: "absolute",
    top: 60,
    right: 20,
    backgroundColor: "rgba(0,0,0,0.6)",
    borderRadius: 16,
    padding: 6,
  },
  topNavOverlay: {
    position: "absolute",
    top: 10,
    left: 20,
    right: 20,
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    zIndex: 10,
  },
  circleBtn: {
    width: 42,
    height: 42,
    borderRadius: 21,
    backgroundColor: "#FFFFFF",
    justifyContent: "center",
    alignItems: "center",
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.15,
    shadowRadius: 6,
    elevation: 3,
  },
  floatingActionRow: {
    position: "absolute",
    bottom: 28,
    alignSelf: "center",
    flexDirection: "row",
    gap: 12,
    zIndex: 10,
  },
  deletePillBtn: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "#EF4444",
    paddingHorizontal: 18,
    paddingVertical: 10,
    borderRadius: 24,
    gap: 6,
  },
  addPhotosPillBtn: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "#18181B",
    paddingHorizontal: 18,
    paddingVertical: 10,
    borderRadius: 24,
    gap: 6,
  },
  pillBtnText: {
    color: "#FFFFFF",
    fontSize: 13,
    fontWeight: "700",
  },
  contentSheet: {
    flex: 1,
    marginTop: -20,
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    paddingHorizontal: 24,
    paddingTop: 28,
    paddingBottom: 40,
  },

  /* VIEW MODE STYLES */
  viewContainer: {
    flex: 1,
  },
  dateHeaderRow: {
    flexDirection: "row",
    justifyContent: "flex-end",
    alignItems: "center",
    marginBottom: 12,
  },
  dateText: {
    fontSize: 15,
    fontWeight: "600",
  },
  viewTitle: {
    fontSize: 28,
    fontWeight: "800",
    lineHeight: 34,
    marginBottom: 16,
  },
  divider: {
    height: 1,
    width: "100%",
    marginVertical: 16,
  },
  sectionHeader: {
    fontSize: 16,
    fontWeight: "700",
    marginBottom: 8,
  },
  viewBodyText: {
    fontSize: 16,
    lineHeight: 24,
  },

  /* EDIT MODE STYLES */
  formContainer: {
    flex: 1,
    gap: 6,
  },
  inputLabel: {
    fontSize: 12,
    fontWeight: "800",
    letterSpacing: 0.8,
    marginTop: 12,
    marginBottom: 6,
  },
  inputCard: {
    borderRadius: 14,
    paddingHorizontal: 16,
    paddingVertical: 14,
    justifyContent: "center",
  },
  noteInputCard: {
    minHeight: 120,
    justifyContent: "flex-start",
  },
  textInput: {
    fontSize: 16,
    padding: 0,
    margin: 0,
  },
  noteTextInput: {
    textAlignVertical: "top",
  },
  saveChangesBtn: {
    borderRadius: 28,
    paddingVertical: 16,
    alignItems: "center",
    justifyContent: "center",
    marginTop: 24,
  },
  saveChangesText: {
    color: "#FFFFFF",
    fontSize: 16,
    fontWeight: "700",
  },

  /* MODAL */
  modalOverlay: {
    flex: 1,
    backgroundColor: "rgba(0,0,0,0.4)",
    justifyContent: "center",
    alignItems: "center",
  },
  modalContent: {
    borderRadius: 20,
    padding: 16,
    width: "85%",
  },
});