#include <string.h>
#include "SevenBitCodec.h"
#include "TypedBinaryWrapper.h"

char encoded[500];
byte decoded[500];

static byte test_data_1 [8] = {
  0x86, 0x4F, 0xD2, 0x6F, 0xB5, 0x59, 0xF7, 0x5B
};

static byte test_data_2 [32] = {
  0x8E, 0x0B, 0xDD, 0x69, 0x76, 0x28, 0xB9, 0x1D,
  0x8F, 0x24, 0x55, 0x87, 0xEE, 0x95, 0xC5, 0xB0,
  0x4D, 0x48, 0x96, 0x3F, 0x79, 0x25, 0x98, 0x77,
  0xB4, 0x9C, 0xD9, 0x06, 0x3A, 0xEA, 0xD3, 0xB7
};

//  --------------------------------------------------------------------------

void setup()
{
  Serial.begin(115200);
}

//  --------------------------------------------------------------------------

void loop() {
  Serial.println("\nBegin test.");

  //  //    encoded = Z85_encode (NULL, 0);
  //  //    decoded = Z85_decode (encoded);
  //  //    Serial.println (streq (encoded, ""));
  //  //    free (decoded);
  //  //    free (encoded);
  //  //
  //  //    encoded = Z85_encode (test_data_1, 3);
  //  //    Serial.println (encoded == NULL);

  // test data 1

  byte *src;
  size_t srcSize;

  for (int idx = 0; idx < 2; idx++) {
    switch (idx) {
      case 0:
        src = test_data_1;
        srcSize = sizeof test_data_1;
        break;
      case 1:
        src = test_data_2;
        srcSize = sizeof test_data_2;
        break;
    }


    for (int i = 0; i < srcSize; i++) {
      Serial.print(src[i], HEX);
      Serial.print("  ");
    }
    Serial.println();

    size_t expected_encoded_size = encodedSize(srcSize);

    encodeBytes( src, srcSize, encoded, sizeof encoded );
    Serial.println(encoded);
    encoded[0] = 0;
    encodeUInt8(src, srcSize, encoded, sizeof encoded);
    Serial.println(encoded);
    encoded[0] = 0;
    encodeInt8((int8_t *)src, srcSize, encoded, sizeof encoded);
    Serial.println(encoded);

    decodeChars(encoded, strlen(encoded), decoded, sizeof decoded);

    for (int i = 0; i < decodedSize(strlen(encoded)); i++) {
      Serial.print(decoded[i], HEX);
      Serial.print("  ");
    }
    Serial.println();
  }

   //  //
  //  //    //  Standard test keys defined by zmq_curve man page
  //  //    byte client_public [32] = {
  //  //        0xBB, 0x88, 0x47, 0x1D, 0x65, 0xE2, 0x65, 0x9B,
  //  //        0x30, 0xC5, 0x5A, 0x53, 0x21, 0xCE, 0xBB, 0x5A,
  //  //        0xAB, 0x2B, 0x70, 0xA3, 0x98, 0x64, 0x5C, 0x26,
  //  //        0xDC, 0xA2, 0xB2, 0xFC, 0xB4, 0x3F, 0xC5, 0x18
  //  //    };
  //  //    byte client_secret [32] = {
  //  //        0x7B, 0xB8, 0x64, 0xB4, 0x89, 0xAF, 0xA3, 0x67,
  //  //        0x1F, 0xBE, 0x69, 0x10, 0x1F, 0x94, 0xB3, 0x89,
  //  //        0x72, 0xF2, 0x48, 0x16, 0xDF, 0xB0, 0x1B, 0x51,
  //  //        0x65, 0x6B, 0x3F, 0xEC, 0x8D, 0xFD, 0x08, 0x88
  //  //    };
  //  //    encoded = Z85_encode (client_public, 32);
  //  ////     puts (encoded);
  //  //    free (encoded);
  //  //    encoded = Z85_encode (client_secret, 32);
  //  ////     puts (encoded);
  //  //    free (encoded);
  //  //
  //  //    byte server_public [32] = {
  //  //        0x54, 0xFC, 0xBA, 0x24, 0xE9, 0x32, 0x49, 0x96,
  //  //        0x93, 0x16, 0xFB, 0x61, 0x7C, 0x87, 0x2B, 0xB0,
  //  //        0xC1, 0xD1, 0xFF, 0x14, 0x80, 0x04, 0x27, 0xC5,
  //  //        0x94, 0xCB, 0xFA, 0xCF, 0x1B, 0xC2, 0xD6, 0x52
  //  //    };
  //  //    byte server_secret [32] = {
  //  //        0x8E, 0x0B, 0xDD, 0x69, 0x76, 0x28, 0xB9, 0x1D,
  //  //        0x8F, 0x24, 0x55, 0x87, 0xEE, 0x95, 0xC5, 0xB0,
  //  //        0x4D, 0x48, 0x96, 0x3F, 0x79, 0x25, 0x98, 0x77,
  //  //        0xB4, 0x9C, 0xD9, 0x06, 0x3A, 0xEA, 0xD3, 0xB7
  //  //    };
  //  //    encoded = Z85_encode (server_public, 32);
  //  ////     puts (encoded);
  //  //    free (encoded);
  //  //    encoded = Z85_encode (server_secret, 32);
  //  ////     puts (encoded);
  //  //    free (encoded);

  Serial.println("End test.");
  delay(2000);
}
