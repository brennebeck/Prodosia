/*
 * Copyright (c) 2018 J.S. Boellaard
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.Bluefix.Prodosia.Imgur.ImgurApi;


import com.Bluefix.Prodosia.Authorization.ImgurAuthorization;
import com.Bluefix.Prodosia.DataType.ImgurKey;
import com.Bluefix.Prodosia.Exception.ExceptionHelper;
import com.Bluefix.Prodosia.GUI.Navigation.VistaNavigator;
import com.Bluefix.Prodosia.Module.ModuleManager;
import com.Bluefix.Prodosia.Storage.KeyStorage;
import com.github.kskelm.baringo.BaringoClient;
import com.github.kskelm.baringo.util.BaringoApiException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Manager for the imgur api.
 */
public class ImgurManager
{
    public static final String EnvVarImgurClientId = "ENV_IMGUR_CLIENT_ID";
    public static final String EnvVarImgurClientSecret = "ENV_IMGUR_CLIENT_SECRET";
    public static final String EnvVarImgurCallback = "ENV_IMGUR_CALLBACK";

    public static final String DefaultImgurCallback = "https://imgur.com";

    //region Singleton

    private static BaringoClient client;

    /**
     * Retrieve the Imgur Api client that allows access to the underlying API.
     * @return The Imgur API client.
     * @throws IOException Exception reading/writing cookies. Non-essential, but good to know anyways.
     * @throws BaringoApiException Baringo framework exception.
     * @throws URISyntaxException Might pertain to an incompatibility with the imgur API.
     */
    public static BaringoClient client() throws IOException, BaringoApiException, URISyntaxException
    {
        if (client == null)
        {
            // first check if there are environment variables for this.
            String envClientId = System.getenv(EnvVarImgurClientId);
            String envClientSecret = System.getenv(EnvVarImgurClientSecret);
            String envCallback = System.getenv(EnvVarImgurCallback);

            ImgurKey key;

            if (envClientId != null && envClientSecret != null)
            {
                if (envCallback == null)
                    envCallback = DefaultImgurCallback;

                key = new ImgurKey(envClientId, envClientSecret, envCallback);
                ImgurManager.client = createClient(key);
                return ImgurManager.client;
            }


            key = KeyStorage.getImgurKey();

            BaringoClient tmpClient = createClient(key);
            initializeClient(tmpClient);
        }

        return client;
    }

    private static BaringoClient createClient(ImgurKey key) throws IOException, BaringoApiException, URISyntaxException
    {
        // get the api key credentials.
        if (key == null)
            return null;

        BaringoClient bClient = null;

        try
        {
            bClient = new BaringoClient.Builder()
                    .clientAuth(key.getClientId(), key.getClientSecret())
                    .build();
        } catch (Exception e)
        {
            ExceptionHelper.showWarning(e);
        }


        return bClient;
    }

    private static void initializeClient(BaringoClient client) throws IOException, URISyntaxException
    {
        if (client == null)
            return;

        // immediately authorize the client if the key was stored locally.
        // note: for anonymous usage it is not necessary to authorize. However,
        // heavy use of authorized api access it expected and immediate authorization
        // determines when the user is prompted, so it can be done on startup.
        ImgurAuthorization.Result res = ImgurAuthorization.authorize(client, new URI(KeyStorage.getImgurKey().getCallback()));

        // if the authorization doesn't complete successfully, redirect
        // the user to the Imgur API keys window and don't remove client
        // initialization.
        if (res == ImgurAuthorization.Result.SUCCESS)
        {
            ImgurManager.client = client;
        }
        else
        {
            ImgurManager.client = null;
        }
    }

    //endregion

    //region Sanitation check


    /**
     * Prompts the Client to re-init
     */
    public static void update() throws IOException, URISyntaxException, BaringoApiException
    {
        boolean clientWasInitialized = client != null;

        ImgurKey key = KeyStorage.getImgurKey();

        BaringoClient tmpClient = createClient(key);

        initializeClient(tmpClient);

        // if the client itself wasn't initialized yet, start the imgur dependencies.
        if (!clientWasInitialized && client != null)
            ModuleManager.startImgurDependencies();
    }

    //endregion


}
