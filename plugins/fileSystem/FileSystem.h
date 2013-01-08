//
//  FileSystem.h
//  Created by Max Woghiren on 2013-01-07.
//

#import <Cordova/CDVPlugin.h>

typedef void(^VoidBlock)(NSString *);

@interface FileSystem : CDVPlugin <UIImagePickerControllerDelegate, UINavigationControllerDelegate>

@property (strong, nonatomic) VoidBlock chooseEntryCallback;

- (void)chooseEntry:(CDVInvokedUrlCommand *)command;

@end
