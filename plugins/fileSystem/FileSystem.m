//
//  FileSystem.m
//  Created by Max Woghiren on 2013-01-07.
//

#import "FileSystem.h"

@implementation FileSystem

@synthesize chooseEntryCallback;

- (void)chooseEntry:(CDVInvokedUrlCommand *)command {
    // Set the callback.
    [self setChooseEntryCallback:[^(NSString *localEntryPath) {
        [[self commandDelegate] sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                                                      messageAsString:localEntryPath]
                                      callbackId:[command callbackId]];
    } copy]];

    // Create and configure the image picker controller.
    UIImagePickerController *imagePickerController = [[UIImagePickerController alloc] init];
    [imagePickerController setDelegate:self];
    [imagePickerController setSourceType:UIImagePickerControllerSourceTypePhotoLibrary];

    // Present the image picker.
    [[self viewController] presentViewController:imagePickerController
                                        animated:YES
                                      completion:nil];
}

#pragma mark UIImagePickerControllerDelegate

- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary *)info {
    // Dismiss the image picker.
    [[self viewController] dismissViewControllerAnimated:YES
                                                completion:nil];

    // Retrieve the selected image and file name.
    UIImage *selectedImage = (UIImage *)[info objectForKey:UIImagePickerControllerOriginalImage];
    NSString *selectedImageName = [(NSURL *)[info objectForKey:UIImagePickerControllerReferenceURL] lastPathComponent];

    // Save the image to the sandbox.
    NSData *selectedImageData = UIImagePNGRepresentation(selectedImage);
    NSString *localEntryPath = [NSTemporaryDirectory() stringByAppendingPathComponent:selectedImageName];
    [selectedImageData writeToFile:localEntryPath
                        atomically:YES];

    // Call the supplied callback.
    [self chooseEntryCallback](localEntryPath);
}

@end
