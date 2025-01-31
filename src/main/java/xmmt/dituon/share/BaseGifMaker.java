package xmmt.dituon.share;

import com.madgag.gif.fmsware.AnimatedGifEncoder;
import com.squareup.gifencoder.GifEncoder;
import com.squareup.gifencoder.Image;
import com.squareup.gifencoder.ImageOptions;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BaseGifMaker {
    public static InputStream makeGIF(ArrayList<AvatarModel> avatarList, ArrayList<TextModel> textList,
                                      HashMap<Short, BufferedImage> stickerMap, boolean antialias) {
        return makeGifUseBufferedStream(avatarList, textList, stickerMap, antialias, null);
    }

    public static InputStream makeGIF(ArrayList<AvatarModel> avatarList, ArrayList<TextModel> textList,
                                      HashMap<Short, BufferedImage> stickerMap,
                                      boolean antialias, Encoder encoder) {
        return makeGIF(avatarList, textList, stickerMap, antialias, null, encoder);
    }

    public static InputStream makeGIF(ArrayList<AvatarModel> avatarList, ArrayList<TextModel> textList,
                                      HashMap<Short, BufferedImage> stickerMap,
                                      boolean antialias, List<Integer> maxSize, Encoder encoder) {
        if (encoder == Encoder.BUFFERED_STREAM) {
            return makeGifUseBufferedStream(avatarList, textList, stickerMap, antialias, maxSize);
        }
        if (encoder == Encoder.ANIMATED_LIB) {
            return makeGifUseAnimatedLib(avatarList, textList, stickerMap, antialias, maxSize);
        }
        if (encoder == Encoder.SQUAREUP_LIB) {
            return makeGifUseSquareupLib(avatarList, textList, stickerMap, antialias, maxSize);
        }
        return null;
    }

    public static InputStream makeGifUseBufferedStream
            (ArrayList<AvatarModel> avatarList, ArrayList<TextModel> textList,
             HashMap<Short, BufferedImage> stickerMap,
             boolean antialias, List<Integer> maxSize) {
        try {
            //遍历获取GIF长度(图片文件数量)
            short i = 0;
            CountDownLatch latch = new CountDownLatch(stickerMap.size());
            HashMap<Short, BufferedImage> imageMap = new HashMap<>();
            for (short key : stickerMap.keySet()) {
                short fi = i++;
                new Thread(() -> {
                    imageMap.put(fi, ImageSynthesis.synthesisImage(
                            stickerMap.get(key), avatarList, textList,
                            antialias, false, fi, maxSize));
                    latch.countDown();
                }).start();
            }

            BufferedGifEncoder gifEncoder =
                    new BufferedGifEncoder(stickerMap.get((short) 0).getType(), 65, true);
            latch.await();
            for (i = 0; i < imageMap.size(); i++) gifEncoder.addFrame(imageMap.get(i));
            gifEncoder.finish();
            return gifEncoder.getOutput();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream makeGifUseAnimatedLib
            (ArrayList<AvatarModel> avatarList, ArrayList<TextModel> textList,
             HashMap<Short, BufferedImage> stickerMap,
             boolean antialias, List<Integer> maxSize) {
        try {
            //遍历获取GIF长度(图片文件数量)
            short i = 0;
            CountDownLatch latch = new CountDownLatch(stickerMap.size());
            HashMap<Short, BufferedImage> imageMap = new HashMap<>();
            for (short key : stickerMap.keySet()) {
                short fi = i++;
                new Thread(() -> {
                    imageMap.put(fi, ImageSynthesis.synthesisImage(
                            stickerMap.get(key), avatarList, textList,
                            antialias, false, fi, maxSize));
                    latch.countDown();
                }).start();
            }

            AnimatedGifEncoder gifEncoder = new AnimatedGifEncoder();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            gifEncoder.start(output);
            gifEncoder.setRepeat(0);
            gifEncoder.setDelay(65);

            latch.await();
            imageMap.forEach((in, image) -> gifEncoder.addFrame(image));
            gifEncoder.finish();
            return new ByteArrayInputStream(output.toByteArray());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream makeGifUseSquareupLib(ArrayList<AvatarModel> avatarList, ArrayList<TextModel> textList,
                                                    HashMap<Short, BufferedImage> stickerMap,
                                                    boolean antialias, List<Integer> maxSize) {
        try {
            short i = 0;
            CountDownLatch latch = new CountDownLatch(stickerMap.size());
            HashMap<Short, Image> imageMap = new HashMap<>();
            int[] size = new int[2];
            for (short key : stickerMap.keySet()) {
                short fi = i++;
                new Thread(() -> {
                    BufferedImage image = ImageSynthesis.synthesisImage(
                            stickerMap.get(key), avatarList, textList,
                            antialias, false, fi, maxSize);
                    if (fi == 0) {
                        size[0] = image.getWidth();
                        size[1] = image.getHeight();
                    }
                    Image rgb = Image.fromRgb(ImageSynthesis.convertImageToArray(image));
                    imageMap.put(fi, rgb);
                    latch.countDown();
                }).start();
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageOptions options = new ImageOptions().setDelay(65, TimeUnit.MILLISECONDS);

            latch.await();
            GifEncoder gifEncoder = new GifEncoder(output, size[0], size[1], 0);
            imageMap.forEach((in, image) -> {
                try {
                    gifEncoder.addImage(image, options);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            gifEncoder.finishEncoding();
            return new ByteArrayInputStream(output.toByteArray());
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream makeGIF(ArrayList<AvatarModel> avatarList, ArrayList<TextModel> textList,
                                      BufferedImage sticker, boolean antialias) {
        return makeGifUseBufferedStream(avatarList, textList, sticker, antialias, null);
    }

    public static InputStream makeGIF(ArrayList<AvatarModel> avatarList, ArrayList<TextModel> textList,
                                      BufferedImage sticker,
                                      boolean antialias, List<Integer> maxSize, Encoder encoder) {
        if (encoder == Encoder.BUFFERED_STREAM) {
            return makeGifUseBufferedStream(avatarList, textList, sticker, antialias, maxSize);
        }
        if (encoder == Encoder.ANIMATED_LIB) {
            return makeGifUseAnimatedLib(avatarList, textList, sticker, antialias, maxSize);
        }
        if (encoder == Encoder.SQUAREUP_LIB) {
            return makeGifUseSquareupLib(avatarList, textList, sticker, antialias, maxSize);
        }
        return null;
    }

    private static InputStream makeGifUseBufferedStream(
            ArrayList<AvatarModel> avatarList, ArrayList<TextModel> textList,
            BufferedImage sticker, boolean antialias, List<Integer> maxSize) {
        try {
            short maxFrameLength = 1;
            for (AvatarModel avatar : avatarList) {
                maxFrameLength = (short) Math.max(maxFrameLength, avatar.getImageList().size());
            }

            CountDownLatch latch = new CountDownLatch(maxFrameLength);
            HashMap<Short, BufferedImage> imageMap = new HashMap<>();
            for (short i = 0; i < maxFrameLength; i++) {
                short fi = i;
                new Thread(() -> {
                    imageMap.put(fi, ImageSynthesis.synthesisImage(
                            sticker, avatarList, textList,
                            antialias, false, fi, maxSize));
                    latch.countDown();
                }).start();
            }

            BufferedGifEncoder gifEncoder =
                    new BufferedGifEncoder(sticker.getType(), 65, true);
            latch.await();
            for (short i = 0; i < imageMap.size(); i++) gifEncoder.addFrame(imageMap.get(i));
            gifEncoder.finish();
            return gifEncoder.getOutput();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream makeGifUseAnimatedLib(
            ArrayList<AvatarModel> avatarList, ArrayList<TextModel> textList,
            BufferedImage sticker, boolean antialias, List<Integer> maxSize) {
        try {
            short maxFrameLength = 1;
            for (AvatarModel avatar : avatarList) {
                maxFrameLength = (short) Math.max(maxFrameLength, avatar.getImageList().size());
            }

            CountDownLatch latch = new CountDownLatch(maxFrameLength);
            HashMap<Short, BufferedImage> imageMap = new HashMap<>();
            for (short i = 0; i < maxFrameLength; i++) {
                short fi = i;
                new Thread(() -> {
                    imageMap.put(fi, ImageSynthesis.synthesisImage(
                            sticker, avatarList, textList,
                            antialias, false, fi, maxSize
                    ));
                    latch.countDown();
                }).start();
            }

            AnimatedGifEncoder gifEncoder = new AnimatedGifEncoder();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            gifEncoder.start(output);
            gifEncoder.setDelay(65);
            gifEncoder.setRepeat(0);

            latch.await();
            imageMap.forEach((i, image) -> gifEncoder.addFrame(image));
            gifEncoder.finish();
            return new ByteArrayInputStream(output.toByteArray());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream makeGifUseSquareupLib(ArrayList<AvatarModel> avatarList, ArrayList<TextModel> textList,
                                                    BufferedImage sticker,
                                                    boolean antialias, List<Integer> maxSize) {
        try {
            short i = 0;
            short maxFrameLength = 1;
            for (AvatarModel avatar : avatarList) {
                maxFrameLength = (short) Math.max(maxFrameLength, avatar.getImageList().size());
            }
            CountDownLatch latch = new CountDownLatch(maxFrameLength);
            HashMap<Short, Image> imageMap = new HashMap<>();
            int[] size = new int[2];
            for (i = 0; i < maxFrameLength; i++) {
                short fi = i;
                new Thread(() -> {
                    BufferedImage image = ImageSynthesis.synthesisImage(
                            sticker, avatarList, textList,
                            antialias, false, fi, maxSize);
                    if (fi == 0) {
                        size[0] = image.getWidth();
                        size[1] = image.getHeight();
                    }
                    Image rgb = Image.fromRgb(ImageSynthesis.convertImageToArray(image));
                    imageMap.put(fi, rgb);
                    latch.countDown();
                }).start();
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageOptions options = new ImageOptions().setDelay(65, TimeUnit.MILLISECONDS);

            latch.await();
            GifEncoder gifEncoder = new GifEncoder(output,
                    size[0], size[1], 0);
            imageMap.forEach((in, image) -> {
                try {
                    gifEncoder.addImage(image, options);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            gifEncoder.finishEncoding();
            return new ByteArrayInputStream(output.toByteArray());
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
