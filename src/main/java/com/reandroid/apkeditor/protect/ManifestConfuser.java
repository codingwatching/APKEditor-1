/*
 *  Copyright (C) 2022 github.com/REAndroid
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.reandroid.apkeditor.protect;

import com.reandroid.arsc.chunk.ChunkType;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.arsc.chunk.xml.ResXmlAttribute;
import com.reandroid.arsc.chunk.xml.ResXmlDocument;
import com.reandroid.arsc.chunk.xml.ResXmlElement;
import com.reandroid.arsc.chunk.xml.ResXmlElementApi;
import com.reandroid.arsc.chunk.xml.UnknownResXmlNode;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.arsc.item.ByteArray;
import com.reandroid.arsc.item.IntegerItem;
import com.reandroid.arsc.item.ResXmlString;
import com.reandroid.arsc.pool.ResXmlStringPool;
import com.reandroid.utils.NumbersUtil;
import com.reandroid.utils.collection.CollectionUtil;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class ManifestConfuser extends Confuser {

    public ManifestConfuser(Protector protector) {
        super(protector, "ManifestConfuser: ");
    }

    @Override
    public void confuse() {
        if (getOptions().skipManifest) {
            logMessage("Skip");
            return;
        }
        AndroidManifestBlock manifestBlock = getApkModule().getAndroidManifest();
        placeBadChunk(manifestBlock);
        confuseAttributes(manifestBlock);
        confuseOffset(manifestBlock);
        manifestBlock.refresh();
    }

    private void confuseAttributes(AndroidManifestBlock manifestBlock) {
        int defaultAttributeSize = 20;
        List<ResXmlElement> elementList = CollectionUtil.toList(manifestBlock.recursiveElements());
        Random random = new Random();
        for (ResXmlElement element : elementList) {
            int size = defaultAttributeSize + random.nextInt(6) + 1;
            element.setAttributesUnitSize(size, false);
            ResXmlAttribute attribute = element.newAttribute();
            attribute.setName(" >\n  </" + element.getName() + ">\n  android:name", 0);
            attribute.setValueAsBoolean(false);
        }
        manifestBlock.getManifestElement().setAttributesUnitSize(
                defaultAttributeSize, false);
    }

    private void confuseOffset(AndroidManifestBlock manifestBlock) {
        ResXmlElement manifest = manifestBlock.getManifestElement();
        Iterator<ResXmlElement> iterator = manifest.getElements();
        while (iterator.hasNext()) {
            confuseOffset(iterator.next());
        }
        confuseOffset(manifest);
    }
    private void confuseOffset(ResXmlElement element) {
        ResXmlElementApi elementApi = new ResXmlElementApi(element);

        int size = elementApi.getAttributeArray().countBytes() + 1;

        ByteArray byteArray = new ByteArray();
        byteArray.setSize(size);
        for (int i = 0; i < size; i = i + 4) {
            byteArray.putInteger(i, -1);
        }
        elementApi.getStartElement()
                .getFirstPlaceHolder()
                .setItem(byteArray);

        element.refresh();
    }
    private void placeBadChunk(AndroidManifestBlock manifestBlock) {
        placeBadChunk(manifestBlock, ChunkType.XML_END_NAMESPACE);
        placeBadChunk(manifestBlock, ChunkType.PACKAGE);
    }
    private void placeBadChunk(AndroidManifestBlock manifestBlock, ChunkType chunkType) {
        UnknownResXmlNode unknown = manifestBlock.newUnknown();
        try {
            unknown.readBytes(new BlockReader(
                    randomStringPool(chunkType)));
        } catch (IOException ignored) {
        }
        manifestBlock.moveTo(unknown, 0);
    }
    private byte[] randomStringPool(ChunkType chunkType) {
        ResXmlDocument document = new ResXmlDocument();
        ResXmlStringPool stringPool = document.getStringPool();

        Random random = new Random();

        int size = NumbersUtil.min(20, 5 + random.nextInt(21));
        stringPool.setUtf8(size % 2 == 0);

        for (int i = 0; i < size; i++) {
            String s = randomString();
            ResXmlString xml = stringPool.getOrCreate(s);
            xml.addReference(new IntegerItem());
        }

        stringPool.refresh();
        stringPool.refresh();
        stringPool.getHeaderBlock().setType(chunkType);

        return stringPool.getBytes();
    }
    private String randomString() {
        StringBuilder builder = new StringBuilder();
        Random random = new Random();
        int size = NumbersUtil.min(100, 15 + random.nextInt(90));
        for (int i = 0; i < size; i++) {
            char c = (char) (10 + random.nextInt(240));
            builder.append(c);
        }
        return builder.toString();
    }
}
