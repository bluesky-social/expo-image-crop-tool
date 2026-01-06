import {
  Alert,
  Button,
  Image,
  SafeAreaView,
  ScrollView,
  Text,
} from "react-native";
import ExpoImageCropTool, {
  OpenCropperOptions,
  OpenCropperResult,
} from "expo-image-crop-tool";
import {
  launchImageLibraryAsync,
  useMediaLibraryPermissions,
} from "expo-image-picker";
import { useState } from "react";

export default function App() {
  const [perms, reqPerms] = useMediaLibraryPermissions();

  const [croppedImg, setCroppedImg] = useState("");
  const [height, setHeight] = useState(0);
  const [width, setWidth] = useState(0);
  const [size, setSize] = useState(0);

  const doCrop = async (options: Omit<OpenCropperOptions, "imageUri">) => {
    if (!perms?.granted) {
      if (!perms?.canAskAgain) {
        Alert.alert(
          "Cannot Request Image Permissions",
          "Unable to re-request image permissions. Please enable them in system preferences"
        );
        return;
      }
      await reqPerms();
    }

    const imgRes = await launchImageLibraryAsync({
      mediaTypes: "images",
    });

    if (!imgRes || !imgRes.assets) {
      throw new Error("failed to get image");
    }

    let res: OpenCropperResult;
    try {
      res = await ExpoImageCropTool.openCropperAsync({
        imageUri: imgRes.assets[0].uri,
        ...options,
      });
    } catch (e: any) {
      Alert.alert("Error!", e.toString());
      return;
    }

    setCroppedImg(res.path);
    setWidth(res.width);
    setHeight(res.height);
    setSize(res.size);
    console.log(res);
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView style={styles.container}>
        <Text style={styles.header}>Image Cropper Example</Text>
        <Button title="Open Cropper Regular" onPress={async () => doCrop({})} />
        <Button
          title="Open Cropper Circle"
          onPress={() => doCrop({ shape: "circle" })}
        />
        <Button
          title="Open Cropper 1:1"
          onPress={() =>
            doCrop({
              aspectRatio: 1 / 1,
            })
          }
        />
        <Button
          title="Open Cropper 16:9"
          onPress={() =>
            doCrop({
              aspectRatio: 16 / 9,
            })
          }
        />
        <Button
          title="Open Cropper 16:9"
          onPress={() =>
            doCrop({
              aspectRatio: 9 / 16,
            })
          }
        />
        <Button
          title="Open Cropper JPEG 50%"
          onPress={() =>
            doCrop({
              aspectRatio: 9 / 16,
              format: "jpeg",
              compressImageQuality: 0.5,
            })
          }
        />
        <Button
          title="Open Cropper JPEG 0.002%"
          onPress={() =>
            doCrop({
              aspectRatio: 9 / 16,
              format: "jpeg",
              compressImageQuality: 0.0002,
            })
          }
        />
        <Button
          title="Custom Text: Save/Back"
          onPress={() =>
            doCrop({
              doneButtonText: "Save",
              cancelButtonText: "Back",
            })
          }
        />
        <Button
          title="Custom Text: Apply/Discard"
          onPress={() =>
            doCrop({
              doneButtonText: "Apply",
              cancelButtonText: "Discard",
            })
          }
        />

        <Text style={styles.header}>Output Image</Text>
        <Text style={styles.text}>
          Width: {width}, Height: {height}, Aspect: {aspect(width, height)},
          Size: {size}
        </Text>
        {croppedImg !== "" && (
          <Image
            source={{ uri: croppedImg }}
            onError={(e) => console.log(e)}
            style={{ aspectRatio: 1, maxWidth: "100%" }}
            resizeMode="contain"
          />
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

// Euclidean GCD
function gcd(a: number, b: number): number {
  return b === 0 ? a : gcd(b, a % b);
}

function aspect(width: number, height: number) {
  const g = gcd(width, height);
  return `${width / g}:${height / g}`;
}

const styles = {
  header: {
    fontSize: 30,
    marginHorizontal: 20,
    marginTop: 40,
    marginBottom: 8,
  },
  text: {
    marginHorizontal: 20,
    marginVertical: 4,
  },
  groupHeader: {
    fontSize: 20,
    marginBottom: 20,
  },
  group: {
    margin: 20,
    backgroundColor: "#fff",
    borderRadius: 10,
    padding: 20,
  },
  container: {
    flex: 1,
    backgroundColor: "#eee",
  },
  view: {
    flex: 1,
    height: 200,
  },
};
